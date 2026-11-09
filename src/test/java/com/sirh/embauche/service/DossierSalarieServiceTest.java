package com.sirh.embauche.service;

import com.sirh.candidat.entity.Document;
import com.sirh.candidat.repository.DocumentRepository;
import com.sirh.commun.enums.StatutContrat;
import com.sirh.commun.enums.TypeContrat;
import com.sirh.commun.enums.TypeDocumentAdministratif;
import com.sirh.commun.enums.ValiditeDocument;
import com.sirh.commun.exception.BusinessException;
import com.sirh.embauche.entity.Contrat;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.embauche.entity.Remuneration;
import com.sirh.embauche.repository.ContratRepository;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.embauche.repository.RemunerationRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DossierSalarieServiceTest {
    final DossierEmployeRepository dossiers = mock(DossierEmployeRepository.class);
    final RemunerationRepository remunerations = mock(RemunerationRepository.class);
    final ContratRepository contrats = mock(ContratRepository.class);
    final DocumentRepository documentsRepo = mock(DocumentRepository.class);
    final DocumentEmployeService documents = mock(DocumentEmployeService.class);
    final DossierSalarieService sut = new DossierSalarieService(dossiers, remunerations, contrats, documentsRepo, documents);
    final DossierEmploye dossier = DossierEmploye.builder().id(1L).nom("Rabe").prenom("Zo")
            .utilisateur(Utilisateur.builder().id(2L).actif(true).build()).build();

    @BeforeEach void init() {
        when(dossiers.verrouillerParId(1L)).thenReturn(Optional.of(dossier));
        when(dossiers.findById(1L)).thenReturn(Optional.of(dossier));
        when(remunerations.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void identiteNeJournaliseQueLesChampsModifies() {
        var f = new DossierSalarieService.IdentiteForm();
        f.setNom("Rabe"); f.setPrenom("Zo"); f.setTelephone("034 00 000 00"); f.setSituationFamiliale("MARIE");
        var modifies = sut.mettreAJourIdentite(1L, f, "rh@sirh.local");
        assertEquals(List.of("Situation familiale", "Téléphone"), modifies);
        assertEquals("rh@sirh.local", dossier.getMisAJourPar());
    }

    @Test void cinDejaUtiliseeRefusee() {
        var f = new DossierSalarieService.IdentiteForm();
        f.setNom("Rabe"); f.setPrenom("Zo"); f.setCin("101");
        when(dossiers.existsByCinAndIdNot("101", 1L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> sut.mettreAJourIdentite(1L, f, "rh"));
    }

    @Test void revisionAjouteUneLigneSansToucherAuxPrecedentes() {
        var debut = LocalDate.of(2026, 1, 1);
        when(contrats.findFirstByDossierIdAndStatut(1L, StatutContrat.EN_COURS)).thenReturn(Optional.of(
                Contrat.builder().id(7L).type(TypeContrat.CDI).statut(StatutContrat.EN_COURS).dateDebut(debut).build()));
        var ancienne = Remuneration.builder().dateEffet(debut).salaireBrut(new BigDecimal("1000000")).build();
        when(remunerations.findByDossierIdOrderByDateEffetDescIdDesc(1L)).thenReturn(List.of(ancienne));

        var r = sut.reviserSalaire(1L, LocalDate.of(2026, 7, 1), new BigDecimal("1200000"), "Augmentation annuelle", "rh");

        assertEquals(7L, r.getContratId());
        assertEquals(new BigDecimal("1000000"), ancienne.getSalaireBrut());
        assertThrows(BusinessException.class, () -> sut.reviserSalaire(1L, LocalDate.of(2025, 12, 1),
                new BigDecimal("900000"), "Erreur", "rh"), "avant le début du contrat");
    }

    @Test void revisionSansContratRefusee() {
        when(contrats.findFirstByDossierIdAndStatut(1L, StatutContrat.EN_COURS)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> sut.reviserSalaire(1L, LocalDate.now(), BigDecimal.TEN, "x", "rh"));
    }

    @Test void validiteCalculeeDepuisLaDateDEmission() {
        var doc = Document.builder().id(3L).dateUpload(LocalDateTime.now()).build();
        when(documents.deposer(any(), eq("CERTIFICAT_RESIDENCE"), any(), eq(1L), isNull(), isNull())).thenReturn(doc);
        sut.ajouterDocument(1L, TypeDocumentAdministratif.CERTIFICAT_RESIDENCE, null, LocalDate.of(2026, 9, 1), null, null);
        assertEquals(LocalDate.of(2026, 12, 1), doc.getDateExpiration());
    }

    @Test void documentAExpirationSansDateRefuse() {
        assertThrows(BusinessException.class, () -> sut.ajouterDocument(1L, TypeDocumentAdministratif.PERMIS_CONDUIRE,
                null, LocalDate.of(2026, 1, 1), null, null));
    }

    @Test void statutsDeValidite() {
        var j = LocalDate.of(2026, 11, 9);
        assertEquals(ValiditeDocument.PERMANENT, ValiditeDocument.de(null, j));
        assertEquals(ValiditeDocument.EXPIRE, ValiditeDocument.de(j.minusDays(1), j));
        assertEquals(ValiditeDocument.A_RENOUVELER, ValiditeDocument.de(j.plusDays(30), j));
        assertEquals(ValiditeDocument.VALIDE, ValiditeDocument.de(j.plusDays(31), j));
    }
}
