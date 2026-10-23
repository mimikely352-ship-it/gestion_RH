package com.sirh.candidat.service;

import com.sirh.commun.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {
    @TempDir Path dossier;

    @Test void refuseUnFauxPdf() {
        var service = new FileStorageService(dossier.toString(), "pdf");
        var fichier = new MockMultipartFile("piece", "document.pdf", "application/pdf", "texte quelconque".getBytes());
        assertThrows(BusinessException.class, () -> service.verifierPdf(fichier));
    }

    @Test void valideStockeEtSupprimeUnPdfDansLeDossierAutorise() {
        var service = new FileStorageService(dossier.toString(), "pdf");
        var fichier = new MockMultipartFile("piece", "document.pdf", "application/pdf", "%PDF-1.4\n".getBytes());
        assertDoesNotThrow(() -> service.verifierPdf(fichier));
        String chemin = service.enregistrer(fichier);
        assertTrue(service.charger(chemin).exists());
        service.supprimer(chemin);
        assertFalse(java.nio.file.Files.exists(dossier.resolve(chemin)));
        assertThrows(IllegalArgumentException.class, () -> service.supprimer("../ailleurs.pdf"));
    }
}
