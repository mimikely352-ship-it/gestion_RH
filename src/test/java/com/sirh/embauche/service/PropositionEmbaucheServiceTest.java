package com.sirh.embauche.service;

import com.sirh.commun.enums.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.demande.repository.DemandeRepository;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.notification.service.NotificationService;
import com.sirh.organisation.entity.Service;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.entity.OffreEmploi;
import com.sirh.recrutement.repository.CandidatureRepository;
import com.sirh.recrutement.repository.OffreEmploiRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PropositionEmbaucheServiceTest {
    private final CandidatureRepository repository = mock(CandidatureRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final DemandeRepository demandes = mock(DemandeRepository.class);
    private final OffreEmploiRepository offres = mock(OffreEmploiRepository.class);
    private final DossierEmployeRepository dossiers = mock(DossierEmployeRepository.class);
    private final EmbaucheService embauche = mock(EmbaucheService.class);
    private final PropositionEmbaucheService service =
            new PropositionEmbaucheService(repository, notifications, demandes, offres, dossiers, embauche);
    private final Service serviceDev = Service.builder().id(5L).nom("Développement").build();

    private Candidature candidature() {
        var utilisateur = com.sirh.utilisateur.entity.Utilisateur.builder().id(4L).email("candidat@example.test").build();
        var offre = OffreEmploi.builder().id(3L).titre("Développeur").nbPostes(1).build();
        var c = Candidature.builder().id(1L).statut(StatutCandidature.RETENUE).offre(offre)
                .candidat(com.sirh.candidat.entity.Candidat.builder().utilisateur(utilisateur).build()).build();
        when(repository.verrouiller(1L)).thenReturn(Optional.of(c));
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(offres.verrouiller(3L)).thenReturn(Optional.of(offre));
        when(embauche.serviceAccueil(eq(c), any())).thenReturn(serviceDev);
        return c;
    }

    private void envoyer(TypeContrat type, BigDecimal salaire, LocalDate debut, LocalDate fin) {
        service.envoyer(1L, type, salaire, debut, fin, Role.EMPLOYE, null, "rh@sirh.local");
    }

    @Test void envoiFigeToutesLesConditionsEtNotifieSansEmbaucher() {
        var c = candidature();
        service.envoyer(1L, TypeContrat.CDI, new BigDecimal("1250.50"), LocalDate.now(), null, Role.CHEF_SERVICE, null, "rh@sirh.local");
        assertEquals(StatutProposition.EN_ATTENTE, c.getPropositionStatut());
        assertEquals(StatutCandidature.RETENUE, c.getStatut());
        assertEquals(Role.CHEF_SERVICE, c.getPropositionRole());
        assertEquals(5L, c.getPropositionServiceId());
        assertEquals("rh@sirh.local", c.getPropositionEnvoyeePar());
        assertNotNull(c.getPropositionEnvoyeeLe());
        verify(notifications).notifier(4L, "Une proposition d’embauche vous attend.", TypeNotification.EMBAUCHE, "/candidat/propositions/1");
        verify(embauche, never()).embaucherALAcceptation(any(), any());
        assertThrows(BusinessException.class, () -> envoyer(TypeContrat.CDI, BigDecimal.TEN, LocalDate.now(), null));
    }

    @Test void seulLeDestinatairePeutLireEtRepondre() {
        var c = candidature(); c.setPropositionStatut(StatutProposition.EN_ATTENTE);
        assertThrows(AccessDeniedException.class, () -> service.consulter(1L, "autre@example.test"));
        assertThrows(AccessDeniedException.class, () -> service.repondre(1L, "autre@example.test", true));
        assertEquals(StatutProposition.EN_ATTENTE, c.getPropositionStatut());
        verifyNoInteractions(notifications);
        verify(embauche, never()).embaucherALAcceptation(any(), any());
    }

    @Test void acceptationCreeLeSalarieEtEstDefinitive() {
        var c = candidature(); c.setPropositionStatut(StatutProposition.EN_ATTENTE);
        var dossier = DossierEmploye.builder().id(7L).matricule("EMP-2026-0007").build();
        when(embauche.embaucherALAcceptation(c, "candidat@example.test")).thenReturn(dossier);

        var reponse = service.repondre(1L, "candidat@example.test", true);

        assertTrue(reponse.embauche());
        assertSame(dossier, reponse.dossier());
        assertEquals(StatutProposition.ACCEPTEE, c.getPropositionStatut());
        assertNotNull(c.getPropositionRepondueLe());
        verify(embauche).embaucherALAcceptation(c, "candidat@example.test");
        assertThrows(BusinessException.class, () -> service.repondre(1L, "candidat@example.test", false));
    }

    @Test void refusNeCreePasDEmbaucheEtLibereLePoste() {
        var c = candidature(); c.setPropositionStatut(StatutProposition.EN_ATTENTE);
        var reponse = service.repondre(1L, "candidat@example.test", false);
        assertFalse(reponse.embauche());
        assertEquals(StatutProposition.REFUSEE, c.getPropositionStatut());
        assertEquals(StatutCandidature.RETENUE, c.getStatut());
        verify(embauche, never()).embaucherALAcceptation(any(), any());
        verify(notifications).notifierRh(contains("autre candidat"), eq(TypeNotification.EMBAUCHE), eq("/rh/candidatures/1"));
    }

    @Test void controleContratSalaireDatesEtRole() {
        candidature();
        assertThrows(BusinessException.class, () -> envoyer(TypeContrat.CDI, BigDecimal.ZERO, LocalDate.now(), null));
        assertThrows(BusinessException.class, () -> envoyer(TypeContrat.CDD, BigDecimal.TEN, LocalDate.now(), null));
        assertThrows(BusinessException.class, () -> envoyer(TypeContrat.CDI, BigDecimal.TEN, LocalDate.now().minusDays(1), null));
        assertThrows(BusinessException.class, () -> envoyer(TypeContrat.CDI, BigDecimal.TEN, LocalDate.now(), LocalDate.now().plusDays(2)));
        assertThrows(BusinessException.class, () -> service.envoyer(1L, TypeContrat.CDI, BigDecimal.TEN, LocalDate.now(), null,
                Role.RH, null, "rh@sirh.local"));
        verify(repository, never()).save(any());
    }

    @Test void plusDePropositionsQueDePostesRefusees() {
        candidature();
        when(repository.countByOffreIdAndPropositionStatut(3L, StatutProposition.EN_ATTENTE)).thenReturn(1L);
        var erreur = assertThrows(BusinessException.class, () -> envoyer(TypeContrat.CDI, BigDecimal.TEN, LocalDate.now(), null));
        assertTrue(erreur.getMessage().contains("pourvus ou proposés"));
        verify(repository, never()).save(any());
    }

    @Test void candidatDejaSalarieRefuse() {
        candidature();
        when(dossiers.findByUtilisateurId(4L)).thenReturn(Optional.of(DossierEmploye.builder().id(2L).build()));
        assertThrows(BusinessException.class, () -> envoyer(TypeContrat.CDI, BigDecimal.TEN, LocalDate.now(), null));
        verify(repository, never()).save(any());
    }

    @Test void budgetGlobalPlafonneLesPropositions() {
        var c = candidature();
        var besoin = com.sirh.demande.entity.Demande.builder().id(7L).budgetGlobalRecrutement(new BigDecimal("30000")).build();
        c.getOffre().setDemandeOrigine(besoin);
        when(demandes.verrouiller(7L)).thenReturn(Optional.of(besoin));
        // 1 000 Ar/mois déjà engagés = 12 000 Ar/an ; reste 18 000 Ar/an.
        when(repository.salairesMensuelsEngages(7L, 1L)).thenReturn(new BigDecimal("1000"));
        assertThrows(BusinessException.class, () -> envoyer(TypeContrat.CDI, new BigDecimal("1500.01"), LocalDate.now(), null));
        verify(repository, never()).save(any());
        envoyer(TypeContrat.CDI, new BigDecimal("1500"), LocalDate.now(), null);
        assertEquals(StatutProposition.EN_ATTENTE, c.getPropositionStatut());
    }

    @Test void candidatureNonRetenueNeRecoitPasDeProposition() {
        var c = candidature(); c.setStatut(StatutCandidature.RECUE);
        assertThrows(BusinessException.class, () -> envoyer(TypeContrat.CDI, BigDecimal.TEN, LocalDate.now(), null));
    }
}
