package com.sirh.embauche.service;

import com.sirh.candidat.entity.Document;
import com.sirh.candidat.repository.CandidatRepository;
import com.sirh.candidat.repository.DocumentRepository;
import com.sirh.candidat.repository.ExperienceRepository;
import com.sirh.commun.enums.Role;
import com.sirh.commun.enums.StatutContrat;
import com.sirh.commun.enums.TypeContrat;
import com.sirh.demande.repository.DemandeRepository;
import com.sirh.embauche.entity.Contrat;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.embauche.repository.*;
import com.sirh.embauche.service.FicheEmployeService.LigneDocumentAdministratif;
import com.sirh.embauche.service.HabilitationService.Habilitation;
import com.sirh.recrutement.repository.CandidatureRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FicheEmployeServiceTest {
    final DossierEmployeRepository dossiers = mock(DossierEmployeRepository.class);
    final DocumentRepository documents = mock(DocumentRepository.class);
    final ContratRepository contrats = mock(ContratRepository.class);
    final FicheEmployeService sut = new FicheEmployeService(dossiers, mock(AffectationRepository.class),
            mock(CorrectionDateEntreeRepository.class), mock(DemandeRepository.class), mock(CandidatRepository.class),
            mock(ExperienceRepository.class), documents, mock(CandidatureRepository.class), contrats,
            mock(DepartRepository.class), mock(RemunerationRepository.class), mock(AuditDossierRepository.class),
            mock(OnboardingService.class));
    final LocalDateTime t0 = LocalDateTime.of(2026, 11, 1, 10, 0);

    @BeforeEach void init() {
        var titulaire = Utilisateur.builder().id(2L).email("e@test.local").role(Role.EMPLOYE).actif(true).build();
        when(dossiers.findById(1L)).thenReturn(Optional.of(DossierEmploye.builder().id(1L).nom("Rabe").prenom("Zo")
                .matricule("EMP-2026-0001").utilisateur(titulaire).build()));
    }

    private static Document piece(long id, String type, LocalDateTime depot) {
        return Document.builder().id(id).type(type).dossierId(1L).dateUpload(depot).nomFichier(type.toLowerCase() + id + ".pdf").build();
    }

    @Test void uneNouvelleCinRemplaceLaPrecedenteMaisLesDiplomesSAjoutent() {
        var cin1 = piece(10L, "CIN", t0);
        var cin2 = piece(11L, "CIN", t0.plusDays(5));
        var bac = piece(12L, "DIPLOME", t0);
        var licence = piece(13L, "DIPLOME", t0); // même seconde : l'ordre de dépôt départage
        when(documents.findByDossierIdOrderByDateUploadDesc(1L)).thenReturn(List.of(cin2, licence, bac, cin1));

        var lignes = sut.obtenir(1L, Habilitation.COMPLETE).getDocumentsAdministratifs().stream()
                .collect(Collectors.toMap(LigneDocumentAdministratif::id, l -> l));

        assertTrue(lignes.get(10L).remplace());
        assertEquals(1, lignes.get(10L).version());
        assertEquals("06/11/2026 à 10:00", lignes.get(10L).remplaceLe());
        assertFalse(lignes.get(11L).remplace());
        assertEquals(2, lignes.get(11L).version());
        assertFalse(lignes.get(12L).remplace(), "un deuxième diplôme ne remplace pas le premier");
        assertFalse(lignes.get(13L).remplace());
        assertEquals(0, lignes.get(12L).version());
    }

    @Test void leRenouvellementEstReliéAuContratPrecedent() {
        var initial = Contrat.builder().id(1L).dossierId(1L).type(TypeContrat.CDD).statut(StatutContrat.EN_COURS)
                .dateDebut(LocalDate.of(2026, 11, 12)).dateFinPrevue(LocalDate.of(2027, 5, 12)).build();
        var renouvellement = Contrat.builder().id(2L).dossierId(1L).type(TypeContrat.CDD).statut(StatutContrat.A_VENIR)
                .dateDebut(LocalDate.of(2027, 5, 13)).dateFinPrevue(LocalDate.of(2027, 11, 12)).contratPrecedentId(1L).build();
        when(contrats.findByDossierIdOrderByDateDebutDescIdDesc(1L)).thenReturn(List.of(renouvellement, initial));

        var fiche = sut.obtenir(1L, Habilitation.COMPLETE);

        assertEquals(1L, fiche.getContratEnCours().id());
        assertEquals(2L, fiche.getContratAVenir().id());
        assertEquals("Renouvelle le CDD du 12/11/2026", fiche.getContratAVenir().precedent());
        assertEquals("Remplacé par le CDD du 13/05/2027 (à venir)", fiche.getContratEnCours().suivant());
    }

    @Test void sansHabilitationDocumentsAucunePieceNEstChargee() {
        when(documents.findByDossierIdOrderByDateUploadDesc(1L)).thenReturn(List.of(piece(10L, "CIN", t0)));
        var fiche = sut.obtenir(1L, Habilitation.PROFESSIONNELLE);
        assertTrue(fiche.getDocumentsAdministratifs().isEmpty());
        assertNull(fiche.getSalaireActuel());
        assertNull(fiche.getCin());
    }
}
