package com.sirh.demande.service;

import com.sirh.commun.enums.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.demande.entity.Demande;
import com.sirh.demande.repository.DemandeRepository;
import com.sirh.demande.repository.ValidationDemandeRepository;
import com.sirh.notification.service.NotificationService;
import com.sirh.organisation.entity.Service;
import com.sirh.organisation.repository.PosteRepository;
import com.sirh.recrutement.repository.OffreEmploiRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DemandeServiceTest {

    private DemandeRepository demandeRepository;
    private ValidationDemandeRepository validationDemandeRepository;
    private UtilisateurRepository utilisateurRepository;
    private DemandeService demandeService;
    private com.sirh.embauche.repository.DossierEmployeRepository dossiers;
    private com.sirh.embauche.repository.AffectationRepository affectations;
    private com.sirh.embauche.service.ParcoursEmployeService parcours;
    private PosteRepository posteRepository;
    private OffreEmploiRepository offreEmploiRepository;
    private NotificationService notificationService;

    private Utilisateur employe;
    private Utilisateur chef;
    private Utilisateur direction;
    private Utilisateur rh;
    private Utilisateur dg;

    @BeforeEach
    void setUp() {
        demandeRepository = Mockito.mock(DemandeRepository.class);
        validationDemandeRepository = Mockito.mock(ValidationDemandeRepository.class);
        utilisateurRepository = Mockito.mock(UtilisateurRepository.class);
        posteRepository = Mockito.mock(PosteRepository.class);
        offreEmploiRepository = Mockito.mock(OffreEmploiRepository.class);
        notificationService = Mockito.mock(NotificationService.class);

        dossiers = Mockito.mock(com.sirh.embauche.repository.DossierEmployeRepository.class);
        affectations = Mockito.mock(com.sirh.embauche.repository.AffectationRepository.class);
        parcours = Mockito.mock(com.sirh.embauche.service.ParcoursEmployeService.class);
        when(utilisateurRepository.verrouiller(Mockito.anyLong())).thenAnswer(i -> utilisateurRepository.findById(i.getArgument(0)));
        demandeService = new DemandeService(demandeRepository, validationDemandeRepository,
                utilisateurRepository, posteRepository, offreEmploiRepository, notificationService,
                Mockito.mock(com.sirh.embauche.service.AffectationAutomatiqueService.class), dossiers, affectations, parcours);
        ReflectionTestUtils.setField(demandeService, "delaiExecutionJoursDefaut", 7);

        chef = Utilisateur.builder().id(2L).email("chef@sirh.local").role(Role.CHEF_SERVICE).actif(true).build();
        Service service = Service.builder().id(1L).nom("Service Test").chefService(chef).build();
        employe = Utilisateur.builder().id(1L).email("employe@sirh.local").role(Role.EMPLOYE).actif(true).service(service).build();
        direction = Utilisateur.builder().id(3L).email("direction@sirh.local").role(Role.DIRECTION).actif(true).build();
        rh = Utilisateur.builder().id(4L).email("rh@sirh.local").role(Role.RH).actif(true).build();
        dg = Utilisateur.builder().id(5L).email("dg@sirh.local").role(Role.DG).actif(true).build();

        when(dossiers.findByUtilisateurId(1L)).thenReturn(Optional.of(com.sirh.embauche.entity.DossierEmploye.builder().id(1L).build()));
        when(affectations.findByDossierIdAndActuelleTrue(1L)).thenReturn(Optional.of(com.sirh.embauche.entity.Affectation.builder()
                .id(77L).poste(com.sirh.organisation.entity.Poste.builder().id(88L).build()).build()));
        Mockito.doAnswer(i -> {
            Demande d = i.getArgument(0);
            d.getAuteur().setService(d.getPosteCible().getSection().getService());
            return null;
        }).when(parcours).appliquerMouvement(Mockito.any(), Mockito.any());
        when(demandeRepository.save(any(Demande.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void transfertDirectAuxRhSansChefEtSansChangementAvantRealisation() {
        employe.setService(null);
        var serviceDestination = Service.builder().id(8L).build();
        var poste = com.sirh.organisation.entity.Poste.builder().id(9L)
                .section(com.sirh.organisation.entity.Section.builder().service(serviceDestination).build()).build();
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(employe));
        when(utilisateurRepository.findById(4L)).thenReturn(Optional.of(rh));
        when(posteRepository.findById(9L)).thenReturn(Optional.of(poste));
        when(offreEmploiRepository.findByStatut(StatutOffre.PUBLIEE)).thenReturn(List.of(
                com.sirh.recrutement.entity.OffreEmploi.builder().poste(poste).build()));
        var demande = demandeService.creer(1L, TypeDemande.TRANSFERT, 9L, "Changer de poste");
        demande.setId(10L);
        when(demandeRepository.findById(10L)).thenReturn(Optional.of(demande));
        Mockito.verify(notificationService).notifierRh(Mockito.anyString(), Mockito.eq(TypeNotification.DEMANDE), Mockito.eq("/rh/demandes"));
        Mockito.verify(notificationService, Mockito.never()).notifier(Mockito.anyLong(), Mockito.anyString(), Mockito.any(), Mockito.eq("/chef-service/demandes"));
        demandeService.valider(10L, 4L, DecisionValidation.VALIDE, null);
        assertEquals(StatutDemande.VALIDEE_RH, demande.getStatut());
        assertNull(employe.getService());
        Mockito.verify(utilisateurRepository, Mockito.never()).findByRole(Role.DG);
        demandeService.marquerEnExecution(10L);
        assertNull(employe.getService());
        demandeService.marquerExecutee(10L);
        assertEquals(StatutDemande.EXECUTEE, demande.getStatut());
        assertSame(serviceDestination, employe.getService());
    }

    @Test
    void chefNePeutPlusDeciderUnTransfertEtLesRhPeuventRefuser() {
        var demande = Demande.builder().id(10L).auteur(employe).type(TypeDemande.TRANSFERT).statut(StatutDemande.VALIDEE_CHEF).build();
        when(demandeRepository.findById(10L)).thenReturn(Optional.of(demande));
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(chef));
        when(utilisateurRepository.findById(4L)).thenReturn(Optional.of(rh));
        assertThrows(BusinessException.class, () -> demandeService.valider(10L, 2L, DecisionValidation.VALIDE, null));
        demandeService.valider(10L, 4L, DecisionValidation.REJETE, "Poste indisponible");
        assertEquals(StatutDemande.REJETEE, demande.getStatut());
        assertThrows(BusinessException.class, () -> demandeService.marquerEnExecution(10L));
    }

    @Test
    void anciennesDemandesDeTransfertSontReprisesParLesRh() {
        var soumise = Demande.builder().type(TypeDemande.TRANSFERT).statut(StatutDemande.SOUMISE).build();
        var chef = Demande.builder().type(TypeDemande.TRANSFERT).statut(StatutDemande.VALIDEE_CHEF).build();
        var accepte = Demande.builder().type(TypeDemande.TRANSFERT).statut(StatutDemande.VALIDEE_RH).build();
        var formation = Demande.builder().type(TypeDemande.FORMATION).statut(StatutDemande.SOUMISE).build();
        when(demandeRepository.aValiderParRh()).thenReturn(List.of(soumise, chef));
        when(demandeRepository.aExecuterParRh()).thenReturn(List.of(accepte));
        assertEquals(List.of(soumise, chef), demandeService.listerPourValidationRh());
        assertEquals(List.of(accepte), demandeService.listerPourExecutionRh());
    }

    @Test
    void creerEchoueSiAucunServiceDeRattachement() {
        Utilisateur sansService = Utilisateur.builder().id(9L).email("x@sirh.local").role(Role.EMPLOYE).actif(true).build();
        when(utilisateurRepository.findById(9L)).thenReturn(Optional.of(sansService));

        assertThrows(BusinessException.class,
                () -> demandeService.creer(9L, TypeDemande.FORMATION, null, "Formation Java"));
    }

    @Test
    void circuitCompletJusquaValidationDG() {
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(employe));
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(chef));
        when(utilisateurRepository.findById(3L)).thenReturn(Optional.of(direction));
        when(utilisateurRepository.findById(4L)).thenReturn(Optional.of(rh));
        when(utilisateurRepository.findById(5L)).thenReturn(Optional.of(dg));
        when(utilisateurRepository.findByRole(Role.DIRECTION)).thenReturn(List.of(direction));
        when(utilisateurRepository.findByRole(Role.RH)).thenReturn(List.of(rh));

        Demande demande = demandeService.creer(1L, TypeDemande.FORMATION, null, "Formation Spring Boot");
        demande.setId(100L);
        when(demandeRepository.findById(100L)).thenReturn(Optional.of(demande));

        demandeService.valider(100L, 2L, DecisionValidation.VALIDE, null); // chef de service
        assertEquals(StatutDemande.VALIDEE_CHEF, demande.getStatut());

        demandeService.valider(100L, 3L, DecisionValidation.VALIDE, null); // direction
        assertEquals(StatutDemande.VALIDEE_DIRECTION, demande.getStatut());

        demandeService.valider(100L, 4L, DecisionValidation.VALIDE, null); // RH
        assertEquals(StatutDemande.VALIDEE_RH, demande.getStatut());

        demandeService.valider(100L, 5L, DecisionValidation.VALIDE, null); // DG
        assertEquals(StatutDemande.VALIDEE_DG, demande.getStatut());
        assertNotNull(demande.getDateLimiteExecution());
    }

    @Test
    void rejetAUneEtapeClotLeCircuit() {
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(employe));
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(chef));

        Demande demande = demandeService.creer(1L, TypeDemande.MATERIEL, null, "Un nouvel ordinateur");
        demande.setId(200L);
        when(demandeRepository.findById(200L)).thenReturn(Optional.of(demande));

        demandeService.valider(200L, 2L, DecisionValidation.REJETE, "Budget insuffisant");

        assertEquals(StatutDemande.REJETEE, demande.getStatut());
    }

    @Test
    void validationParUnNonHabiliteEstRefusee() {
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(employe));
        when(utilisateurRepository.findById(4L)).thenReturn(Optional.of(rh)); // RH essaie de valider à l'étape chef

        Demande demande = demandeService.creer(1L, TypeDemande.FORMATION, null, "Formation");
        demande.setId(300L);
        when(demandeRepository.findById(300L)).thenReturn(Optional.of(demande));

        assertThrows(BusinessException.class,
                () -> demandeService.valider(300L, 4L, DecisionValidation.VALIDE, null));
    }
}
