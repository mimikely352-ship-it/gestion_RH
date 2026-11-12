package com.sirh.presence.service;
import com.sirh.presence.entity.*;
import com.sirh.presence.repository.*;
import com.sirh.commun.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlanningServiceTest {
    private final RegimeTravailRepository regimes=mock(RegimeTravailRepository.class);
    private final JourPlanningRepository jours=mock(JourPlanningRepository.class);
    private final DemandeCongeRepository conges=mock(DemandeCongeRepository.class);
    private final AbsenceRepository absences=mock(AbsenceRepository.class);
    private final PointageJourRepository pointages=mock(PointageJourRepository.class);
    private final AccesPresenceService acces=mock(AccesPresenceService.class);
    private final Clock clock=Clock.fixed(Instant.parse("2026-11-09T09:00:00Z"),ZoneId.of("Africa/Nairobi"));
    private final PlanningService service=new PlanningService(regimes,jours,conges,absences,pointages,acces,clock);
    private final UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken("rh@test","");
    @Test void regimeDateEtExceptionNeChangentPasLesJoursPrecedents() {
        var debut=LocalDate.of(2026,11,9);
        when(regimes.findByDossierIdOrderByDepuisDesc(1L)).thenReturn(List.of(RegimeTravail.builder()
                .depuis(debut.plusDays(1)).debut(LocalTime.of(8,0)).fin(LocalTime.of(16,0)).pauseMinutes(30).jours("1,2,3,4,5").build()));
        when(jours.findByDossierIdAndJourBetween(1L,debut,debut.plusDays(2))).thenReturn(List.of(JourPlanning.builder().jour(debut.plusDays(2)).repos(true).build()));
        var planning=service.horaires(1L,debut,debut.plusDays(2));
        assertEquals(480,planning.get(debut).minutes());
        assertEquals(LocalTime.of(9,0),planning.get(debut).getDebut());
        assertEquals(450,planning.get(debut.plusDays(1)).minutes());
        assertTrue(planning.get(debut.plusDays(2)).isRepos());
        assertEquals(0,planning.get(debut.plusDays(2)).minutes());
    }
    @Test void weekendParDefautEtControlesHoraire() {
        assertTrue(service.jour(1L,LocalDate.of(2026,11,14)).isRepos());
        assertThrows(BusinessException.class,() -> PlanningService.verifierHoraire(LocalTime.of(18,0),LocalTime.of(9,0),0));
        assertThrows(BusinessException.class,() -> PlanningService.verifierHoraire(LocalTime.of(9,0),LocalTime.of(10,0),60));
    }
    @Test void regimeIntermediaireNeModifiePasLesReservesDuRegimeSuivant() {
        var debut = LocalDate.now(clock);
        var prochain = debut.plusDays(7);
        when(regimes.findByDossierIdOrderByDepuisDesc(1L)).thenReturn(List.of(RegimeTravail.builder().depuis(prochain).build()));
        when(conges.findByDossierIdAndDebutLessThanEqualAndFinGreaterThanEqual(eq(1L),any(),eq(debut)))
                .thenAnswer(i -> ((LocalDate)i.getArgument(1)).isBefore(prochain) ? List.of()
                        : List.of(DemandeConge.builder().statut(StatutConge.ACCEPTE).build()));
        assertDoesNotThrow(() -> service.regime(1L,debut,LocalTime.of(9,0),LocalTime.of(18,0),60,Set.of(1,2,3,4,5),auth));
        verify(regimes).save(any());
        verify(conges).findByDossierIdAndDebutLessThanEqualAndFinGreaterThanEqual(1L,prochain.minusDays(1),debut);
    }
    @Test void bloqueModificationDUnPlanningReserveOuDejaPointe() {
        var date=LocalDate.now(clock);
        when(conges.findByDossierIdAndDebutLessThanEqualAndFinGreaterThanEqual(1L,date,date))
                .thenReturn(List.of(DemandeConge.builder().statut(StatutConge.ATTENTE_RH).build()));
        assertThrows(BusinessException.class,() -> service.exception(1L,date,true,null,null,0,auth));
        verify(jours,never()).save(any());
        assertThrows(BusinessException.class,() -> service.regime(1L,date.minusDays(1),LocalTime.of(9,0),LocalTime.of(18,0),60,Set.of(1),auth));
    }
}
