package com.sirh.recrutement.controller;

import com.sirh.candidat.repository.CandidatRepository;
import com.sirh.commun.enums.StatutCandidature;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.recrutement.dto.CandidatureDto;
import com.sirh.recrutement.service.CandidatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CandidatureApiController {

    private final CandidatureService candidatureService;
    private final CandidatRepository candidatRepository;

    @PostMapping("/api/offres/{offreId}/candidatures")
    @PreAuthorize("hasRole('CANDIDAT')")
    public CandidatureDto postuler(@PathVariable Long offreId, Authentication authentication) {
        Long candidatId = candidatRepository.findByUtilisateurEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Profil candidat introuvable.")).getId();
        return new CandidatureDto(candidatureService.postuler(candidatId, offreId));
    }

    @GetMapping("/api/candidats/moi/candidatures")
    @PreAuthorize("hasRole('CANDIDAT')")
    public List<CandidatureDto> mesCandidatures(Authentication authentication) {
        Long candidatId = candidatRepository.findByUtilisateurEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Profil candidat introuvable.")).getId();
        return candidatureService.listerPourCandidat(candidatId).stream().map(CandidatureDto::new).toList();
    }

    @GetMapping("/api/offres/{offreId}/candidatures")
    @PreAuthorize("hasRole('RH')")
    public List<CandidatureDto> candidaturesDeOffre(@PathVariable Long offreId) {
        return candidatureService.listerPourOffre(offreId).stream().map(CandidatureDto::new).toList();
    }

    @PutMapping("/api/candidatures/{id}/statut")
    @PreAuthorize("hasRole('RH')")
    public CandidatureDto changerStatut(@PathVariable Long id, @RequestParam StatutCandidature statut) {
        if (statut != StatutCandidature.ACCEPTEE_ENTRETIEN && statut != StatutCandidature.NON_RETENUE) {
            throw new com.sirh.commun.exception.BusinessException("Utilisez les actions du parcours pour les autres décisions.");
        }
        return new CandidatureDto(candidatureService.changerStatut(id, statut));
    }
}
