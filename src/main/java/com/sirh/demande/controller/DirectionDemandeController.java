package com.sirh.demande.controller;

import com.sirh.commun.enums.DecisionValidation;
import com.sirh.commun.enums.StatutDemande;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.demande.repository.DemandeRepository;
import com.sirh.demande.service.DemandeService;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/direction/demandes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DIRECTION')")
public class DirectionDemandeController {

    private final DemandeService demandeService;
    private final DemandeRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("demandes", demandeRepository.findByStatut(StatutDemande.VALIDEE_CHEF).stream()
                .filter(d -> d.getType() != com.sirh.commun.enums.TypeDemande.TRANSFERT).toList());
        return "direction/demandes-liste";
    }

    @PostMapping("/{id}/valider")
    public String valider(@PathVariable Long id, Authentication authentication,
                           @RequestParam DecisionValidation decision, @RequestParam(required = false) String commentaire) {
        Long moiId = utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable.")).getId();
        demandeService.valider(id, moiId, decision, commentaire);
        return "redirect:/direction/demandes";
    }
}
