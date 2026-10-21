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

    @GetMapping("/documents/{id}/telecharger")
    @PreAuthorize("hasAnyRole('RH','CANDIDAT')")
    public ResponseEntity<Resource> telecharger(@PathVariable Long id, Authentication authentication) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + id));

        boolean estRh = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RH"));
        boolean estProprietaire = document.getCandidat() != null
                && document.getCandidat().getUtilisateur().getEmail().equals(authentication.getName());
        if (!estRh && !estProprietaire) {
            throw new AccessDeniedException("Accès refusé à ce document.");
        }

        Resource ressource = fileStorageService.charger(document.getChemin());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getNomFichier() + "\"")
                .body(ressource);
    }
}
