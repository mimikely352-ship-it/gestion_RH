package com.sirh.embauche.service;

import com.sirh.candidat.entity.Document;
import com.sirh.candidat.repository.DocumentRepository;
import com.sirh.candidat.service.FileStorageService;
import com.sirh.commun.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Dépôt des pièces du dossier employé (contrat signé, lettre de démission...). PDF uniquement. */
@Service
@RequiredArgsConstructor
public class DocumentEmployeService {
    private final FileStorageService stockage;
    private final DocumentRepository documents;

    public Document deposer(MultipartFile fichier, String type, String libelle, Long dossierId, Long contratId, Long departId) {
        if (fichier == null || fichier.isEmpty()) throw new BusinessException("Choisissez le fichier PDF à déposer.");
        stockage.verifierPdf(fichier);
        String chemin = stockage.enregistrer(fichier);
        String nom = fichier.getOriginalFilename() == null ? "document.pdf" : fichier.getOriginalFilename();
        return documents.save(Document.builder()
                .type(type).libelle(libelle)
                .nomFichier(nom.length() > 255 ? nom.substring(nom.length() - 255) : nom)
                .chemin(chemin).mime("application/pdf").taille(fichier.getSize())
                .dossierId(dossierId).contratId(contratId).departId(departId)
                .build());
    }
}
