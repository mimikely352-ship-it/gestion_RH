package com.sirh.embauche.service;

import com.sirh.candidat.entity.Document;
import com.sirh.candidat.repository.DocumentRepository;
import com.sirh.commun.enums.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.embauche.entity.Affectation;
import com.sirh.embauche.entity.Contrat;
import com.sirh.embauche.entity.Depart;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.embauche.repository.AffectationRepository;
import com.sirh.embauche.repository.ContratRepository;
import com.sirh.embauche.repository.DepartRepository;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DepartServiceTest {
    final DepartRepository departs = mock(DepartRepository.class);
    final DossierEmployeRepository dossiers = mock(DossierEmployeRepository.class);
    final ContratRepository contrats = mock(ContratRepository.class);
    final AffectationRepository affectations = mock(AffectationRepository.class);
    final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    final DocumentRepository documents = mock(DocumentRepository.class);
    final DepartService sut = new DepartService(departs, dossiers, contrats, affectations, utilisateurs, documents,
            mock(DocumentEmployeService.class));

    final Utilisateur utilisateur = Utilisateur.builder().id(2L).email("e@test.local").actif(true).build();
    final DossierEmploye dossier = DossierEmploye.builder().id(1L).utilisateur(utilisateur).build();

    private Depart depart(TypeDepart type, LocalDate effet) {
        var d = Depart.builder().id(9L).dossier(dossier).type(type).statut(StatutDepart.EN_COURS)
                .motif(type == TypeDepart.LICENCIEMENT ? "FAUTE_GRAVE" : null)
                .dateNotification(effet.minusDays(30)).dateEffet(effet).creeLe(LocalDateTime.now()).creePar("rh").build();
        when(departs.findById(9L)).thenReturn(Optional.of(d));
        return d;
    }

    private static Document piece(PieceDepart p) { return Document.builder().type(p.name()).departId(9L).build(); }

    @Test void renvoiSansMotifRefuse() {
        when(dossiers.verrouillerParId(1L)).thenReturn(Optional.of(dossier));
        when(departs.findFirstByDossierIdAndStatut(1L, StatutDepart.EN_COURS)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> sut.ouvrir(1L, TypeDepart.LICENCIEMENT, null, null,
                LocalDate.now(), LocalDate.now().plusDays(10), "rh"));
        verify(departs, never()).save(any());
    }

    @Test void clotureRefuseeSiPieceObligatoireManquante() {
        depart(TypeDepart.DEMISSION, LocalDate.now().minusDays(1));
        when(documents.findByDepartId(9L)).thenReturn(List.of(piece(PieceDepart.LETTRE_DEMISSION)));
        var erreur = assertThrows(BusinessException.class, () -> sut.cloturer(1L, 9L, "rh"));
        assertTrue(erreur.getMessage().contains("Certificat de travail"));
        assertTrue(utilisateur.isActif());
    }

    @Test void clotureRefuseeAvantLaDateDEffet() {
        depart(TypeDepart.DEMISSION, LocalDate.now().plusDays(5));
        assertThrows(BusinessException.class, () -> sut.cloturer(1L, 9L, "rh"));
    }

    @Test void clotureRompLeContratTermineLAffectationEtDesactiveLeCompte() {
        var effet = LocalDate.now();
        var d = depart(TypeDepart.LICENCIEMENT, effet);
        when(documents.findByDepartId(9L)).thenReturn(List.of(piece(PieceDepart.CONVOCATION_ENTRETIEN),
                piece(PieceDepart.LETTRE_LICENCIEMENT), piece(PieceDepart.CERTIFICAT_TRAVAIL), piece(PieceDepart.SOLDE_TOUT_COMPTE)));
        var contrat = Contrat.builder().type(TypeContrat.CDI).statut(StatutContrat.EN_COURS).dateDebut(effet.minusYears(1)).build();
        when(contrats.findFirstByDossierIdAndStatut(1L, StatutContrat.EN_COURS)).thenReturn(Optional.of(contrat));
        var affectation = Affectation.builder().actuelle(true).dateDebut(effet.minusYears(1)).build();
        when(affectations.findByDossierIdAndActuelleTrue(1L)).thenReturn(Optional.of(affectation));

        sut.cloturer(1L, 9L, "rh@sirh.local");

        assertEquals(StatutContrat.ROMPU, contrat.getStatut());
        assertEquals(effet, contrat.getDateFinEffective());
        assertTrue(contrat.getMotifFin().contains("Faute grave"));
        assertFalse(affectation.isActuelle());
        assertEquals(effet, affectation.getDateFin());
        assertFalse(utilisateur.isActif());
        assertNotNull(utilisateur.getTokensValidesDepuis());
        assertEquals(StatutDepart.CLOTURE, d.getStatut());
    }

    @Test void pieceDUnAutreTypeDeDepartRefusee() {
        depart(TypeDepart.DEMISSION, LocalDate.now());
        assertThrows(BusinessException.class, () -> sut.deposerPiece(1L, 9L, PieceDepart.LETTRE_LICENCIEMENT, null));
    }
}
