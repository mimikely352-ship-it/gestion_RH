package com.sirh.presence.service;
import com.sirh.presence.entity.*;
import com.sirh.presence.repository.*;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.commun.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TempsServiceTest {
    private final AccesPresenceService acces=mock(AccesPresenceService.class);
    private final PlanningService planning=mock(PlanningService.class);
    private final PointageJourRepository pointages=mock(PointageJourRepository.class);
    private final AbsenceRepository absences=mock(AbsenceRepository.class);
    private final Clock clock=Clock.fixed(Instant.parse("2026-11-09T17:00:00Z"),ZoneId.of("Africa/Nairobi"));
    private final TempsService service=new TempsService(acces,planning,pointages,absences,clock);
    private final UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken("emp@test","");
    private final LocalDate date=LocalDate.now(clock);
    @BeforeEach void init() {
        var d=DossierEmploye.builder().id(1L).dateEntree(date.minusDays(10)).build();
        when(acces.personnel(auth)).thenReturn(d); when(acces.verrouiller(1L)).thenReturn(d); when(acces.lire(1L,auth)).thenReturn(d);
        var h=new PlanningService.Horaire(LocalTime.of(9,0),LocalTime.of(18,0),60,false);
        when(planning.jour(1L,date)).thenReturn(h); when(planning.horaires(1L,date,date)).thenReturn(Map.of(date,h));
    }
    @Test void calculeTempsNetRetardEtDepassementSansValidationAutomatique() {
        var p=PointageJour.builder().jour(date).entree(date.atTime(9,15)).sortie(date.atTime(19,15)).pauseMinutes(60)
                .minutesPrevues(480).debutPrevu(LocalTime.of(9,0)).build();
        when(pointages.findByDossierIdAndJourBetweenOrderByJourAsc(1L,date,date)).thenReturn(List.of(p));
        var j=service.calendrier(1L,date,date,auth).get(0);
        assertEquals(540,j.getTravaillees()); assertEquals(15,j.getRetard()); assertEquals(60,j.getSupplementaires()); assertEquals(0,j.getValidees());
    }
    @Test void entreeFigeLePlanningEtInterditDoublePointage() {
        service.entrer(auth);
        var capture=org.mockito.ArgumentCaptor.forClass(PointageJour.class); verify(pointages).save(capture.capture());
        assertEquals(480,capture.getValue().getMinutesPrevues()); assertEquals(LocalDateTime.now(clock),capture.getValue().getEntree());
        when(pointages.findByDossierIdAndJour(1L,date)).thenReturn(Optional.of(capture.getValue()));
        assertThrows(BusinessException.class,() -> service.entrer(auth));
    }
    @Test void absenceValideeBloqueEntreeEtAlimenteMinutesNonPayees() {
        var a=Absence.builder().debut(date).fin(date).libelle("Sans solde").remuneree(false).active(true).build();
        when(absences.findByDossierIdAndActiveTrueAndDebutLessThanEqualAndFinGreaterThanEqual(1L,date,date)).thenReturn(List.of(a));
        assertThrows(BusinessException.class,() -> service.entrer(auth));
        var j=service.calendrier(1L,date,date,auth).get(0);
        assertEquals(0,j.getPrevues()); assertEquals(480,j.getAbsenceNonPayee()); assertEquals("Sans solde",j.getAbsence());
    }
    @Test void sortieSansEntreeEtCorrectionIncoherenteRefusees() {
        assertThrows(BusinessException.class,() -> service.sortir(0,auth));
        assertThrows(BusinessException.class,() -> service.corriger(1L,date,LocalTime.of(9,0),LocalTime.of(18,0),60,1,"Correction",auth));
        assertThrows(BusinessException.class,() -> service.corriger(1L,date,LocalTime.of(18,0),LocalTime.of(9,0),0,0,"Correction",auth));
        verify(pointages,never()).save(any());
    }
    @Test void sortieDeduitLaPauseDeclaree() {
        var p=PointageJour.builder().jour(date).entree(date.atTime(9,0)).minutesPrevues(480).build();
        when(pointages.findFirstByDossierIdAndSortieIsNullOrderByJourAsc(1L)).thenReturn(Optional.of(p));
        service.sortir(60,auth);
        assertEquals(600,TempsService.travaillees(p)); assertEquals(120,TempsService.supplementaires(p));
        assertEquals(0,p.getSupplementairesValidees());
    }
    @Test void periodeDeConsultationBornee() {
        assertThrows(BusinessException.class,() -> service.calendrier(1L,date,date.plusDays(62),auth));
    }
    @Test void calendrierConserveLaDureeFigeeAuPointage() {
        var p=PointageJour.builder().jour(date).entree(date.atTime(8,0)).sortie(date.atTime(16,0))
                .pauseMinutes(30).minutesPrevues(450).debutPrevu(LocalTime.of(8,0)).build();
        when(pointages.findByDossierIdAndJourBetweenOrderByJourAsc(1L,date,date)).thenReturn(List.of(p));
        var j=service.calendrier(1L,date,date,auth).get(0);
        assertEquals(450,j.getPrevues());
        assertEquals(450,j.getTravaillees());
        assertEquals(0,j.getSupplementaires());
    }
    @Test void pointageOuvertLaVeilleBloqueLEntreeAvecUnMessageClair() {
        var hier=PointageJour.builder().jour(date.minusDays(1)).entree(date.minusDays(1).atTime(9,0)).minutesPrevues(480).build();
        when(pointages.findFirstByDossierIdAndSortieIsNullOrderByJourAsc(1L)).thenReturn(Optional.of(hier));
        var erreur=assertThrows(BusinessException.class,() -> service.entrer(auth));
        assertEquals("Votre pointage du 08/11/2026 n’a pas de sortie : les RH doivent le corriger avant une nouvelle entrée.",erreur.getMessage());
        var s=service.situationDuJour(auth);
        assertSame(hier,s.getOuvertAnterieur()); assertFalse(s.isEntreePossible()); assertFalse(s.isSortiePossible());
    }
    @Test void situationDuJourNOffreQueLActionPossible() {
        assertTrue(service.situationDuJour(auth).isEntreePossible(), "rien de pointé : entrée possible");
        var ouvert=PointageJour.builder().jour(date).entree(date.atTime(9,0)).minutesPrevues(480).build();
        when(pointages.findFirstByDossierIdAndSortieIsNullOrderByJourAsc(1L)).thenReturn(Optional.of(ouvert));
        var s=service.situationDuJour(auth); assertTrue(s.isSortiePossible()); assertFalse(s.isEntreePossible());
        when(pointages.findFirstByDossierIdAndSortieIsNullOrderByJourAsc(1L)).thenReturn(Optional.empty());
        ouvert.setSortie(date.atTime(17,0));
        when(pointages.findByDossierIdAndJour(1L,date)).thenReturn(Optional.of(ouvert));
        s=service.situationDuJour(auth); assertSame(ouvert,s.getTermineDuJour()); assertFalse(s.isEntreePossible());
        when(pointages.findByDossierIdAndJour(1L,date)).thenReturn(Optional.empty());
        when(absences.findByDossierIdAndActiveTrueAndDebutLessThanEqualAndFinGreaterThanEqual(1L,date,date))
                .thenReturn(List.of(Absence.builder().debut(date).fin(date).libelle("Congé annuel").active(true).build()));
        s=service.situationDuJour(auth); assertEquals("Congé annuel",s.getAbsence()); assertFalse(s.isEntreePossible());
    }
    @Test void sortieLeLendemainExigeUneCorrectionRh() {
        var p=PointageJour.builder().jour(date.minusDays(1)).entree(date.minusDays(1).atTime(21,0))
                .minutesPrevues(480).build();
        when(pointages.findFirstByDossierIdAndSortieIsNullOrderByJourAsc(1L)).thenReturn(Optional.of(p));
        assertThrows(BusinessException.class,() -> service.sortir(60,auth));
        assertNull(p.getSortie());
        verify(pointages,never()).save(any());
    }
}
