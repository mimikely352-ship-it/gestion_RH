package com.sirh.recrutement.service;

import com.sirh.candidat.entity.Candidat;
import com.sirh.candidat.entity.Document;
import com.sirh.candidat.repository.CandidatRepository;
import com.sirh.candidat.repository.DocumentRepository;
import com.sirh.commun.enums.StatutCandidature;
import com.sirh.commun.enums.StatutOffre;
import com.sirh.commun.exception.BusinessException;
import com.sirh.notification.service.NotificationService;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.entity.OffreEmploi;
import com.sirh.recrutement.repository.CandidatureRepository;
import com.sirh.referentiel.entity.Diplome;
import com.sirh.utilisateur.entity.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CandidatureServiceTest {
    private final CandidatureRepository repository = mock(CandidatureRepository.class);
    private final CandidatRepository candidats = mock(CandidatRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final OffreEmploiService offres = mock(OffreEmploiService.class);
    private final MatchingService matching = mock(MatchingService.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private CandidatureService service;
    private Candidat candidat;
    private OffreEmploi offre;

    @BeforeEach void init() {
        service = new CandidatureService(repository, candidats, documents, offres, matching, notifications);
        candidat = Candidat.builder().id(1L).utilisateur(Utilisateur.builder().id(2L).role(com.sirh.commun.enums.Role.CANDIDAT).build()).build();
        offre = OffreEmploi.builder().id(3L).titre("Java").statut(StatutOffre.PUBLIEE)
                .entreprise(com.sirh.organisation.entity.Entreprise.builder().id(10L).nom("Entreprise A").build()).build();
        when(candidats.verrouiller(1L)).thenReturn(Optional.of(candidat));
        when(offres.obtenir(3L)).thenReturn(offre);
        when(documents.findFirstByCandidatIdAndTypeOrderByDateUploadDesc(1L, "CV")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void scoreFaibleNeRefusePasLeDossier() {
        when(matching.calculerScore(candidat, offre)).thenReturn(BigDecimal.ZERO);
        assertEquals(StatutCandidature.RECUE, service.postuler(1L, 3L).getStatut());
    }

    @Test void refuseUneAutreOffreDeLaMemeEntreprise() {
        when(repository.existsByCandidatIdAndOffreEntrepriseId(1L, 10L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.postuler(1L, 3L));
        verify(repository, never()).save(any());
        verifyNoInteractions(matching, notifications);
    }

    @Test void autoriseUneEntrepriseDifferente() {
        when(repository.existsByCandidatIdAndOffreEntrepriseId(1L, 10L)).thenReturn(true);
        offre.setEntreprise(com.sirh.organisation.entity.Entreprise.builder().id(20L).nom("Entreprise B").build());
        when(matching.calculerScore(candidat, offre)).thenReturn(BigDecimal.ZERO);
        assertEquals(StatutCandidature.RECUE, service.postuler(1L, 3L).getStatut());
        verify(repository).existsByCandidatIdAndOffreEntrepriseId(1L, 20L);
    }

    @Test void refuseUnEmployeEtUneOffreSansEntreprise() {
        candidat.getUtilisateur().setRole(com.sirh.commun.enums.Role.EMPLOYE);
        assertThrows(BusinessException.class, () -> service.postuler(1L, 3L));
        candidat.getUtilisateur().setRole(com.sirh.commun.enums.Role.CANDIDAT);
        offre.setEntreprise(null);
        assertThrows(BusinessException.class, () -> service.postuler(1L, 3L));
        verify(repository, never()).save(any());
    }

    @Test void impossibleDeContournerLesPiecesRequisesParLeDepotSansFichiers() {
        offre.getPiecesRequises().add("Mon justificatif");
        assertThrows(BusinessException.class, () -> service.postuler(1L, 3L));
        verify(repository, never()).save(any());
        verifyNoInteractions(notifications);
    }

    @Test void conserveLesPiecesExigeesDansLeDossier() {
        offre.getPiecesRequises().add("Mon justificatif");
        Document piece = Document.builder().id(8L).libelle("Mon justificatif").candidat(candidat).build();
        when(matching.calculerScore(candidat, offre)).thenReturn(BigDecimal.TEN);
        var dossier = service.postulerAvecPieces(1L, 3L, java.util.Set.of(piece));
        assertTrue(dossier.getPiecesComplementaires().contains(piece));
        assertEquals(StatutCandidature.RECUE, dossier.getStatut());
    }

    @Test void refuseLaPieceDunAutreCandidat() {
        Document piece = Document.builder().id(8L).libelle("Justificatif")
                .candidat(Candidat.builder().id(99L).build()).build();
        assertThrows(BusinessException.class, () -> service.postulerAvecPieces(1L, 3L, java.util.Set.of(piece)));
        verify(repository, never()).save(any());
    }

    @Test void depotNotifieLeCandidatEtLesRhAvecLeLienDuDossier() {
        when(repository.save(any())).thenAnswer(i -> {
            Candidature dossier = i.getArgument(0);
            dossier.setId(42L);
            return dossier;
        });
        when(matching.calculerScore(candidat, offre)).thenReturn(BigDecimal.TEN);
        service.postuler(1L, 3L);
        verify(notifications).notifier(eq(2L), contains("reçue"), any(), eq("/candidat/mes-candidatures"));
        verify(notifications).notifierRh(contains("Java"), any(), eq("/rh/candidatures/42"));
    }

    @Test void scoreEleveNeSelectionnePasLeDossier() {
        when(matching.calculerScore(candidat, offre)).thenReturn(BigDecimal.valueOf(100));
        assertEquals(StatutCandidature.RECUE, service.postuler(1L, 3L).getStatut());
    }

    @Test void conserveCvEtSelectionDeDiplomesAuDepot() {
        Document cv = Document.builder().id(4L).build();
        Diplome diplome = new Diplome();
        candidat.getDiplomes().add(diplome);
        when(documents.findFirstByCandidatIdAndTypeOrderByDateUploadDesc(1L, "CV")).thenReturn(Optional.of(cv));
        when(matching.calculerScore(candidat, offre)).thenReturn(BigDecimal.TEN);
        Candidature dossier = service.postuler(1L, 3L);
        candidat.getDiplomes().clear();
        assertSame(cv, dossier.getCvDocument());
        assertTrue(dossier.getDiplomes().contains(diplome));
    }

    @Test void preselectionNotifieEtNePeutPasEtreRejouee() {
        Candidature dossier = Candidature.builder().candidat(candidat).offre(offre).build();
        when(repository.findById(5L)).thenReturn(Optional.of(dossier));
        service.changerStatut(5L, StatutCandidature.ACCEPTEE_ENTRETIEN);
        verify(notifications).notifier(eq(2L), contains("retenue pour un entretien"), any(), any());
        assertThrows(BusinessException.class, () -> service.changerStatut(5L, StatutCandidature.NON_RETENUE));
    }

    @Test void refuseLeSautDirectVersEmbauche() {
        when(repository.findById(5L)).thenReturn(Optional.of(Candidature.builder().build()));
        assertThrows(BusinessException.class, () -> service.changerStatut(5L, StatutCandidature.EMBAUCHE));
        verify(repository, never()).save(any());
    }
}
