package com.sirh.presence.service;
import com.sirh.presence.entity.*;
import com.sirh.presence.repository.*;
import com.sirh.commun.enums.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.candidat.service.FileStorageService;
import com.sirh.notification.service.NotificationService;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CongeServiceTest {
    private final DemandeCongeRepository demandes=mock(DemandeCongeRepository.class);
    private final DroitCongeRepository droits=mock(DroitCongeRepository.class);
    private final TypeCongeRepository types=mock(TypeCongeRepository.class);
    private final AbsenceRepository absences=mock(AbsenceRepository.class);
    private final PointageJourRepository pointages=mock(PointageJourRepository.class);
    private final PlanningService planning=mock(PlanningService.class);
    private final AccesPresenceService acces=mock(AccesPresenceService.class);
    private final FileStorageService stockage=mock(FileStorageService.class);
    private final NotificationService notifications=mock(NotificationService.class);
    private final Clock clock=Clock.fixed(Instant.parse("2026-11-09T09:00:00Z"),ZoneId.of("Africa/Nairobi"));
    private final CongeService service=new CongeService(demandes,droits,types,absences,pointages,planning,acces,stockage,notifications,clock);
    private final Authentication emp=new UsernamePasswordAuthenticationToken("emp","");
    private final Authentication chef=new UsernamePasswordAuthenticationToken("chef","");
    private final Authentication rh=new UsernamePasswordAuthenticationToken("rh","");
    private final Authentication autre=new UsernamePasswordAuthenticationToken("autre","");
    private final TypeConge annuel=TypeConge.builder().id(1L).nom("Annuel").deduitSolde(true).remunere(true).actif(true).build();
    private final TypeConge sansSolde=TypeConge.builder().id(2L).nom("Sans solde").actif(true).build();
    private final List<DemandeConge> registre=new ArrayList<>();
    private final List<Absence> registreAbsences=new ArrayList<>();
    private final LocalDate debut=LocalDate.of(2026,11,10), fin=LocalDate.of(2026,11,16);
    private DossierEmploye dossier;
    @BeforeEach void init() {
        ReflectionTestUtils.setField(service,"validationManager",true);
        var utilisateur=Utilisateur.builder().id(1L).email("emp").actif(true).role(Role.EMPLOYE).build();
        dossier=DossierEmploye.builder().id(1L).utilisateur(utilisateur).nom("Test").prenom("Employé").dateEntree(LocalDate.of(2020,1,1)).build();
        when(acces.personnel(emp)).thenReturn(dossier); when(acces.verrouiller(1L)).thenReturn(dossier);
        when(acces.dossier(1L)).thenReturn(dossier); when(acces.lire(eq(1L),any())).thenReturn(dossier);
        when(acces.responsable(dossier)).thenReturn(2L);
        when(acces.utilisateur(any())).thenAnswer(i -> switch(((Authentication)i.getArgument(0)).getName()) {
            case "emp" -> utilisateur;
            case "chef" -> Utilisateur.builder().id(2L).role(Role.CHEF_SERVICE).actif(true).build();
            case "rh" -> Utilisateur.builder().id(3L).role(Role.RH).actif(true).build();
            default -> Utilisateur.builder().id(4L).role(Role.CHEF_SERVICE).actif(true).build();
        });
        when(types.findById(1L)).thenReturn(Optional.of(annuel)); when(types.findById(2L)).thenReturn(Optional.of(sansSolde));
        when(types.findByActifTrueOrderByNomAsc()).thenReturn(List.of(annuel,sansSolde));
        when(droits.findByDossierIdAndTypeIdAndAnnee(1L,1L,2026)).thenReturn(Optional.of(DroitConge.builder().jours(10).build()));
        when(demandes.save(any())).thenAnswer(i -> { DemandeConge d=i.getArgument(0); if(d.getId()==null){d.setId((long)registre.size()+1);registre.add(d);} return d; });
        when(demandes.findById(anyLong())).thenAnswer(i -> registre.stream().filter(d -> d.getId().equals(i.getArgument(0))).findFirst());
        when(demandes.dossierId(anyLong())).thenAnswer(i -> registre.stream().filter(d -> d.getId().equals(i.getArgument(0))).map(DemandeConge::getDossierId).findFirst());
        when(demandes.findByDossierIdAndDebutLessThanEqualAndFinGreaterThanEqual(eq(1L),any(),any())).thenAnswer(i -> {
            LocalDate f=i.getArgument(1),d=i.getArgument(2);
            return registre.stream().filter(c -> !c.getDebut().isAfter(f) && !c.getFin().isBefore(d)).toList();
        });
        when(absences.save(any())).thenAnswer(i -> { Absence a=i.getArgument(0);if(a.getId()==null){a.setId((long)registreAbsences.size()+1);registreAbsences.add(a);}return a; });
        when(absences.findByDemandeId(anyLong())).thenAnswer(i -> registreAbsences.stream().filter(a -> Objects.equals(a.getDemandeId(),i.getArgument(0))).findFirst());
        when(absences.findByDossierIdAndActiveTrueAndDebutLessThanEqualAndFinGreaterThanEqual(eq(1L),any(),any())).thenAnswer(i -> {
            LocalDate f=i.getArgument(1),d=i.getArgument(2);
            return registreAbsences.stream().filter(a -> a.isActive() && !a.getDebut().isAfter(f) && !a.getFin().isBefore(d)).toList();
        });
        when(planning.horaires(eq(1L),any(),any())).thenAnswer(i -> {
            LocalDate d=i.getArgument(1),f=i.getArgument(2); var m=new LinkedHashMap<LocalDate,PlanningService.Horaire>();
            for(var date=d;!date.isAfter(f);date=date.plusDays(1))m.put(date,new PlanningService.Horaire(LocalTime.of(9,0),LocalTime.of(18,0),60,date.getDayOfWeek().getValue()>5));
            return m;
        });
    }
    private void demander() { service.soumettre(1L,debut,fin,"Repos",null,emp); }
    @Test void reserveUniquementLesJoursPlanifiesEtBloqueChevauchement() {
        demander(); assertEquals(5,registre.get(0).getJours()); assertEquals(StatutConge.ATTENTE_MANAGER,registre.get(0).getStatut());
        var solde=service.soldes(1L,2026,emp).get(0);
        assertEquals(5,solde.getReserves()); assertEquals(5,solde.getDisponible());
        assertThrows(BusinessException.class,this::demander); verify(acces,atLeastOnce()).verrouiller(1L);
    }
    @Test void soldeReserveNePeutPasEtreDepenseDeuxFois() {
        demander(); service.soumettre(1L,debut.plusDays(7),fin.plusDays(7),"Repos",null,emp);
        assertEquals(0,service.soldes(1L,2026,emp).get(0).getDisponible());
        assertThrows(BusinessException.class,() -> service.soumettre(1L,debut.plusDays(14),fin.plusDays(14),"Repos",null,emp));
    }
    @Test void doubleValidationCreeAbsenceEtDebiteUneSeuleFois() {
        demander(); assertThrows(AccessDeniedException.class,() -> service.decider(1L,true,null,rh));
        service.decider(1L,true,null,chef); assertEquals(StatutConge.ATTENTE_RH,registre.get(0).getStatut()); assertTrue(registreAbsences.isEmpty());
        service.decider(1L,true,null,rh); assertEquals(StatutConge.ACCEPTE,registre.get(0).getStatut());
        assertEquals(1,registreAbsences.size()); assertTrue(registreAbsences.get(0).isRemuneree());
        var solde=service.soldes(1L,2026,emp).get(0); assertEquals(0,solde.getReserves());assertEquals(5,solde.getConsommes());assertEquals(5,solde.getDisponible());
        assertThrows(AccessDeniedException.class,() -> service.decider(1L,true,null,rh)); assertEquals(1,registreAbsences.size());
    }
    @Test void commentaireDuResponsableResteDistinctDeLaDecisionRh() {
        demander(); service.decider(1L,true,"  Relais organisé dans le service  ",chef);
        assertEquals("Relais organisé dans le service",registre.get(0).getCommentaireManager());
        service.decider(1L,true,"  Accord RH  ",rh);
        assertEquals("Relais organisé dans le service",registre.get(0).getCommentaireManager());
        assertEquals("Accord RH",registre.get(0).getCommentaire());
        assertEquals(StatutConge.ACCEPTE,registre.get(0).getStatut());
    }
    @Test void refusEtAnnulationLiberentLaReservation() {
        demander(); service.decider(1L,false,"Période indisponible",chef);
        assertEquals(10,service.soldes(1L,2026,emp).get(0).getDisponible());
        demander(); service.annuler(2L,"Changement de dates",emp);
        assertEquals(10,service.soldes(1L,2026,emp).get(0).getDisponible());
    }
    @Test void rhAnnuleUnCongeFuturEtRetireLAbsence() {
        demander();service.decider(1L,true,null,chef);service.decider(1L,true,null,rh);
        assertThrows(BusinessException.class,() -> service.annuler(1L,"Annulation",emp));
        service.annuler(1L,"Annulation convenue",rh);
        assertFalse(registreAbsences.get(0).isActive()); assertEquals(10,service.soldes(1L,2026,emp).get(0).getDisponible());
        assertNotNull(registre.get(0).getDecisionLe()); assertNotNull(registre.get(0).getAnnuleeLe());
    }
    @Test void notificationsLisiblesAChaqueEtape() {
        demander(); service.decider(1L,true,null,chef);
        verify(notifications).notifier(eq(1L),eq("Votre demande de congé du 10/11/2026 au 16/11/2026 est validée par votre responsable ; elle attend la décision des RH."),
                eq(TypeNotification.DEMANDE),eq("/conges"));
        service.decider(1L,false,"Période chargée",rh);
        verify(notifications).notifier(eq(1L),eq("Votre demande de congé du 10/11/2026 au 16/11/2026 est refusée : Période chargée"),
                eq(TypeNotification.DEMANDE),eq("/conges"));
    }
    @Test void annulationParLesRhConserveLeMotifEtPrevientLeSalarie() {
        demander(); service.decider(1L,true,null,chef); service.decider(1L,true,null,rh);
        var annulee=service.annuler(1L,"  Réorganisation du service  ",rh);
        assertEquals(StatutConge.ANNULE,annulee.getStatut());
        assertEquals("Réorganisation du service",annulee.getMotifAnnulation());
        verify(notifications).notifier(eq(1L),eq("Votre congé du 10/11/2026 au 16/11/2026 a été annulé par les RH : Réorganisation du service"),
                eq(TypeNotification.DEMANDE),eq("/conges"));
    }
    @Test void annulationParLeSalarieNeLePrevientPas() {
        demander(); service.annuler(1L,"Changement de projet",emp);
        assertEquals("Changement de projet",registre.get(0).getMotifAnnulation());
        verify(notifications,never()).notifier(eq(1L),contains("annulé par les RH"),any(),any());
    }
    @Test void autreResponsableEtDemandeurNePeuventPasValider() {
        demander();assertThrows(AccessDeniedException.class,() -> service.decider(1L,true,null,autre));
        assertThrows(AccessDeniedException.class,() -> service.decider(1L,true,null,emp));
        assertThrows(AccessDeniedException.class,() -> service.annuler(1L,"Annuler",autre));
    }
    @Test void droitsNePeuventPasDescendreSousLesReservations() {
        demander();assertThrows(BusinessException.class,() -> service.definirDroit(1L,1L,2026,4,"Révision",rh));
        verify(droits,never()).save(any());
    }
    @Test void justificatifRequisEtDatesInvalidesSontRefuses() {
        annuel.setJustificatifRequis(true);assertThrows(BusinessException.class,this::demander);
        assertThrows(BusinessException.class,() -> service.soumettre(2L,LocalDate.of(2026,12,31),LocalDate.of(2027,1,2),"Repos",null,emp));
        assertThrows(BusinessException.class,() -> service.soumettre(2L,debut.minusDays(10),fin,"Repos",null,emp));
        assertTrue(registre.isEmpty());verify(stockage,never()).enregistrer(any());
    }
    @Test void politiqueCalendairesEtValidationRhSeuleConfigurables() {
        ReflectionTestUtils.setField(service,"joursCalendaires",true);ReflectionTestUtils.setField(service,"validationManager",false);
        service.soumettre(2L,debut,fin,"Sans solde",null,emp);
        assertEquals(7,registre.get(0).getJours());assertEquals(StatutConge.ATTENTE_RH,registre.get(0).getStatut());
        service.decider(1L,true,null,rh);assertFalse(registreAbsences.get(0).isRemuneree());
    }
    @Test void absenceManuelleRespecteLesPointagesEtCongesExistants() {
        demander();assertThrows(BusinessException.class,() -> service.absence(1L,debut,fin,"Maladie",false,null,rh));
        when(pointages.findByDossierIdAndJourBetweenOrderByJourAsc(1L,debut.plusDays(7),fin.plusDays(7)))
                .thenReturn(List.of(PointageJour.builder().build()));
        assertThrows(BusinessException.class,() -> service.absence(1L,debut.plusDays(7),fin.plusDays(7),"Maladie",false,null,rh));
        verify(absences,never()).save(any());
    }
}
