package com.sirh.demande.controller;

import com.sirh.commun.enums.DecisionValidation;
import com.sirh.commun.enums.TypeDemande;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.demande.dto.DemandeDto;
import com.sirh.demande.service.DemandeService;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
public class DemandeApiController {

    private final DemandeService demandeService;
    private final UtilisateurRepository utilisateurRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYE','CHEF_SERVICE','DIRECTION','RH','DG')")
    public DemandeDto creer(Authentication authentication, @RequestParam TypeDemande type,
                             @RequestParam(required = false) Long posteCibleId, @RequestParam String description) {
        Long auteurId = utilisateurId(authentication);
        return new DemandeDto(demandeService.creer(auteurId, type, posteCibleId, description));
    }

    @GetMapping("/moi")
    public List<DemandeDto> mesDemandes(Authentication authentication) {
        Long auteurId = utilisateurId(authentication);
        return demandeService.listerPourAuteur(auteurId).stream().map(DemandeDto::new).toList();
    }

    @PutMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('CHEF_SERVICE','DIRECTION','RH','DG')")
    public DemandeDto valider(@PathVariable Long id, Authentication authentication,
                               @RequestParam DecisionValidation decision, @RequestParam(required = false) String commentaire) {
        Long valideurId = utilisateurId(authentication);
        return new DemandeDto(demandeService.valider(id, valideurId, decision, commentaire));
    }

    private Long utilisateurId(Authentication authentication) {
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable.")).getId();
    }
}
