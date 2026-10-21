package com.sirh.demande.controller;

import com.sirh.commun.enums.TypeDemande;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.demande.service.DemandeService;
import com.sirh.organisation.repository.PosteRepository;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/employe/demandes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('EMPLOYE','CHEF_SERVICE','DIRECTION','RH','DG')")
public class EmployeDemandeController {

    private final DemandeService demandeService;
    private final UtilisateurRepository utilisateurRepository;
    private final PosteRepository posteRepository;

    @GetMapping
    public String liste(Authentication authentication, Model model) {
        Long auteurId = utilisateurConnecte(authentication).getId();
        model.addAttribute("demandes", demandeService.listerPourAuteur(auteurId));
        return "employe/demandes-liste";
    }

    @GetMapping("/nouvelle")
    public String formulaire(Model model) {
        model.addAttribute("postes", posteRepository.findAll());
        model.addAttribute("types", TypeDemande.values());
        return "employe/demande-form";
    }

    @PostMapping
    public String creer(Authentication authentication, @RequestParam TypeDemande type,
                         @RequestParam(required = false) Long posteCibleId,
                         @RequestParam String description, Model model) {
        try {
            Long auteurId = utilisateurConnecte(authentication).getId();
            demandeService.creer(auteurId, type, posteCibleId, description);
            return "redirect:/employe/demandes";
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("postes", posteRepository.findAll());
            model.addAttribute("types", TypeDemande.values());
            return "employe/demande-form";
        }
    }

    private com.sirh.utilisateur.entity.Utilisateur utilisateurConnecte(Authentication authentication) {
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }
}
