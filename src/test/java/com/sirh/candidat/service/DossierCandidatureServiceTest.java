package com.sirh.candidat.service;

import com.sirh.candidat.entity.Candidat;
import com.sirh.candidat.entity.Document;
import com.sirh.candidat.repository.CandidatRepository;
import com.sirh.candidat.repository.DocumentRepository;
import com.sirh.commun.exception.BusinessException;
import com.sirh.recrutement.entity.OffreEmploi;
import com.sirh.recrutement.service.CandidatureService;
import com.sirh.recrutement.service.OffreEmploiService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DossierCandidatureServiceTest {
    @Test void neCopieRienSiUnePieceObligatoireManque() {
        var candidats = mock(CandidatRepository.class);
        var documents = mock(DocumentRepository.class);
        var offres = mock(OffreEmploiService.class);
        var candidatures = mock(CandidatureService.class);
        var stockage = mock(FileStorageService.class);
        when(candidats.findById(1L)).thenReturn(Optional.of(Candidat.builder().id(1L).build()));
        when(offres.obtenir(2L)).thenReturn(OffreEmploi.builder().piecesRequises(Set.of("Justificatif")).build());
        var service = new DossierCandidatureService(candidats, documents, offres, candidatures, stockage);
        assertThrows(BusinessException.class, () -> service.deposer(1L, 2L, Map.of()));
        verifyNoInteractions(documents, candidatures, stockage);
    }

    @Test void nettoieLesFichiersSiLaTransactionDeDepotEstAnnulee() {
        var candidats = mock(CandidatRepository.class);
        var documents = mock(DocumentRepository.class);
        var offres = mock(OffreEmploiService.class);
        var candidatures = mock(CandidatureService.class);
        var stockage = mock(FileStorageService.class);
        when(candidats.findById(1L)).thenReturn(Optional.of(Candidat.builder().id(1L).build()));
        when(offres.obtenir(2L)).thenReturn(OffreEmploi.builder().piecesRequises(Set.of("Justificatif")).build());
        when(stockage.enregistrer(any())).thenReturn("test.pdf");
        when(documents.save(any())).thenAnswer(i -> (Document) i.getArgument(0));
        when(candidatures.postulerAvecPieces(eq(1L), eq(2L), any())).thenThrow(new BusinessException("Offre fermée"));
        var service = new DossierCandidatureService(candidats, documents, offres, candidatures, stockage);
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThrows(BusinessException.class, () -> service.deposer(1L, 2L,
                    Map.of("piece_0", new MockMultipartFile("piece_0", "piece.pdf", "application/pdf", "%PDF-1.4".getBytes()))));
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verify(stockage).supprimer("test.pdf");
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
}
