package com.sirh.candidat.controller;

import com.sirh.candidat.entity.Document;
import com.sirh.candidat.repository.DocumentRepository;
import com.sirh.candidat.service.FileStorageService;
import com.sirh.commun.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DocumentDownloadController {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final com.sirh.embauche.repository.DossierEmployeRepository dossiers;
    private final com.sirh.embauche.service.HabilitationService habilitations;
    private final com.sirh.embauche.service.AuditDossierService audit;

    @GetMapping("/documents/{id}/telecharger")
    @PreAuthorize("isAuthenticated()")
    // Cette méthode sert à télécharger ou afficher un document, par exemple le CV d’un candidat.
    public ResponseEntity<Resource> telecharger(@PathVariable Long id, Authentication authentication) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + id));

        // Document rattaché à un dossier salarié (directement, ou via le dossier candidat du salarié) :
        // l'accès suit l'habilitation « documents » et chaque consultation est journalisée.
        var dossier = document.getDossierId() != null ? dossiers.findById(document.getDossierId()).orElse(null)
                : document.getCandidat() != null ? dossiers.findByUtilisateurId(document.getCandidat().getUtilisateur().getId()).orElse(null)
                : null;
        boolean estRh = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RH"));
        boolean estProprietaire = document.getCandidat() != null
                && document.getCandidat().getUtilisateur().getEmail().equals(authentication.getName());
        boolean habilite = dossier != null && habilitations.pour(authentication, dossier).documents();
        if (!estRh && !estProprietaire && !habilite) {
            throw new AccessDeniedException("Accès refusé à ce document.");
        }
        if (dossier != null)
            audit.journaliser(dossier.getId(), authentication, com.sirh.commun.enums.ActionAudit.DOCUMENT_CONSULTE,
                    (document.getLibelle() == null ? document.getType() : document.getLibelle()) + " — " + document.getNomFichier());

        Resource ressource = fileStorageService.charger(document.getChemin());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, org.springframework.http.ContentDisposition.inline()
                        .filename(document.getNomFichier(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(ressource);
    }
}
