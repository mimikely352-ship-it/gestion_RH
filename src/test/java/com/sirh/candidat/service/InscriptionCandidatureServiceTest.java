package com.sirh.candidat.service;

import com.sirh.candidat.dto.InscriptionCandidatForm;
import com.sirh.candidat.entity.Candidat;
import com.sirh.commun.enums.StatutOffre;
import com.sirh.commun.exception.BusinessException;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.entity.OffreEmploi;
import com.sirh.recrutement.service.OffreEmploiService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InscriptionCandidatureServiceTest {
    final CandidatInscriptionService inscriptions = mock(CandidatInscriptionService.class);
    final DossierCandidatureService dossiers = mock(DossierCandidatureService.class);
    final OffreEmploiService offres = mock(OffreEmploiService.class);
    final FileStorageService stockage = mock(FileStorageService.class);
    final InscriptionCandidatureService service = new InscriptionCandidatureService(inscriptions,dossiers,offres,stockage);

    @Test void offreExpireeNeCreeAucunCompte() {
        when(offres.obtenirPublique(2L)).thenReturn(OffreEmploi.builder().statut(StatutOffre.PUBLIEE).dateLimite(LocalDate.now().minusDays(1)).build());
        assertThrows(BusinessException.class, () -> service.inscrireEtPostuler(2L,new InscriptionCandidatForm(),Map.of()));
        verifyNoInteractions(inscriptions,dossiers,stockage);
    }

    @Test void pieceManquanteNeCreeAucunCompte() {
        when(offres.obtenirPublique(2L)).thenReturn(OffreEmploi.builder().statut(StatutOffre.PUBLIEE).piecesRequises(Set.of("Portfolio")).build());
        assertThrows(BusinessException.class, () -> service.inscrireEtPostuler(2L,new InscriptionCandidatForm(),Map.of()));
        verifyNoInteractions(inscriptions,dossiers);
    }

    @Test void inscriptionDeposeSurLOffreChoisieAvecLesPieces() {
        var form = new InscriptionCandidatForm();
        form.setEmail("candidat@example.com"); form.setMotDePasse("motdepasse123"); form.setNom("Nom"); form.setPrenom("Prenom");
        var cv = new MockMultipartFile("cv","cv.pdf","application/pdf","%PDF-1.4".getBytes());
        form.setCv(cv);
        Map<String,org.springframework.web.multipart.MultipartFile> pieces = Map.of("piece_0",cv);
        when(offres.obtenirPublique(2L)).thenReturn(OffreEmploi.builder().statut(StatutOffre.PUBLIEE).piecesRequises(Set.of("Portfolio")).build());
        when(inscriptions.inscrire(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any())).thenReturn(Candidat.builder().id(7L).build());
        var resultat = Candidature.builder().id(9L).build();
        when(dossiers.deposer(7L,2L,pieces)).thenReturn(resultat);
        assertSame(resultat,service.inscrireEtPostuler(2L,form,pieces));
        var ordre = inOrder(stockage,inscriptions,dossiers);
        ordre.verify(stockage,times(2)).verifierPdf(cv);
        ordre.verify(inscriptions).inscrire(eq(form.getEmail()),eq(form.getMotDePasse()),eq("Nom"),eq("Prenom"),isNull(),isNull(),isNull(),isNull(),isNull(),eq(List.of()),isNull(),eq(cv));
        ordre.verify(dossiers).deposer(7L,2L,pieces);
    }
}
