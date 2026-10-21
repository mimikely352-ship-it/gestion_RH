package com.sirh.demande.controller;

import com.sirh.commun.enums.DecisionValidation;
import com.sirh.commun.enums.StatutDemande;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.demande.entity.Demande;
import com.sirh.demande.repository.DemandeRepository;
import com.sirh.demande.service.DemandeService;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/chef-service/demandes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CHEF_SERVICE')")
public class ChefServiceDemandeController {

    private final DemandeService demandeService;
    private final DemandeRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    public String liste(Authentication authentication, Model model) {
        Long moiId = utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable.")).getId();
        List<Demande> aValider = demandeRepository.findByStatut(StatutDemande.SOUMISE).stream()
                .filter(d -> d.getAuteur().getService() != null
                        && d.getAuteur().getService().getChefService() != null
                        && d.getAuteur().getService().getChefService().getId().equals(moiId))
                .toList();
        model.addAttribute("demandes", aValider);
        return "chef-service/demandes-liste";
    }

    @PostMapping("/{id}/valider")
    public String valider(@PathVariable Long id, Authentication authentication,
                           @RequestParam DecisionValidation decision, @RequestParam(required = false) String commentaire) {
        Long moiId = utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable.")).getId();
        demandeService.valider(id, moiId, decision, commentaire);
        return "redirect:/chef-service/demandes";
    }
}
