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
        PosteRepository posteRepository = Mockito.mock(PosteRepository.class);
        OffreEmploiRepository offreEmploiRepository = Mockito.mock(OffreEmploiRepository.class);
        NotificationService notificationService = Mockito.mock(NotificationService.class);

        demandeService = new DemandeService(demandeRepository, validationDemandeRepository,
                utilisateurRepository, posteRepository, offreEmploiRepository, notificationService);
        ReflectionTestUtils.setField(demandeService, "delaiExecutionJoursDefaut", 7);

        chef = Utilisateur.builder().id(2L).email("chef@sirh.local").role(Role.CHEF_SERVICE).actif(true).build();
        Service service = Service.builder().id(1L).nom("Service Test").chefService(chef).build();
        employe = Utilisateur.builder().id(1L).email("employe@sirh.local").role(Role.EMPLOYE).actif(true).service(service).build();
        direction = Utilisateur.builder().id(3L).email("direction@sirh.local").role(Role.DIRECTION).actif(true).build();
        rh = Utilisateur.builder().id(4L).email("rh@sirh.local").role(Role.RH).actif(true).build();
        dg = Utilisateur.builder().id(5L).email("dg@sirh.local").role(Role.DG).actif(true).build();

        when(demandeRepository.save(any(Demande.class))).thenAnswer(inv -> inv.getArgument(0));
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
