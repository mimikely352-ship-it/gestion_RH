package com.sirh.candidat.service;

import com.sirh.candidat.entity.Document;
import com.sirh.candidat.repository.CandidatRepository;
import com.sirh.candidat.repository.DocumentRepository;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.service.CandidatureService;
import com.sirh.recrutement.service.OffreEmploiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DossierCandidatureService {
    private final CandidatRepository candidats;
    private final DocumentRepository documents;
    private final OffreEmploiService offres;
    private final CandidatureService candidatures;
    private final FileStorageService stockage;

    @Transactional
    public Candidature deposer(Long candidatId, Long offreId, Map<String, MultipartFile> fichiers) {
        var candidat = candidats.findById(candidatId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil candidat introuvable."));
        var pieces = offres.obtenir(offreId).getListePiecesRequises();
        // Valider l'ensemble avant de commencer les copies de fichiers.
        for (int i = 0; i < pieces.size(); i++) {
            var fichier = fichiers.get("piece_" + i);
            if (fichier == null || fichier.isEmpty()) throw new BusinessException("Pièce manquante : " + pieces.get(i));
            stockage.verifierPdf(fichier);
        }
        var chemins = new ArrayList<String>();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int statut) {
                    if (statut != STATUS_COMMITTED) chemins.forEach(stockage::supprimer);
                }
            });
        }
        var piecesEnregistrees = new HashSet<Document>();
        for (int i = 0; i < pieces.size(); i++) {
            var fichier = fichiers.get("piece_" + i);
            String chemin = stockage.enregistrer(fichier);
            chemins.add(chemin);
            String nom = Optional.ofNullable(fichier.getOriginalFilename()).orElse("document.pdf")
                    .replace('\\', '/');
            nom = nom.substring(nom.lastIndexOf('/') + 1);
            if (nom.length() > 255) nom = nom.substring(nom.length() - 255);
            piecesEnregistrees.add(documents.save(Document.builder().type("COMPLEMENT")
                    .libelle(pieces.get(i)).candidat(candidat).nomFichier(nom).chemin(chemin)
                    .mime("application/pdf").taille(fichier.getSize()).build()));
        }
        return candidatures.postulerAvecPieces(candidatId, offreId, piecesEnregistrees);
    }
}
