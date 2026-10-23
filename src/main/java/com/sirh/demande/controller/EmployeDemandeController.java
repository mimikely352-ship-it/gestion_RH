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
@PreAuthorize("hasRole('EMPLOYE')")
public class EmployeDemandeController {

    private final DemandeService demandeService;
    private final UtilisateurRepository utilisateurRepository;
    private final PosteRepository posteRepository;
    private final com.sirh.embauche.service.AffectationAutomatiqueService affectationAutomatique;

    @GetMapping
    public String liste(Authentication authentication, Model model) {
        Long auteurId = utilisateurConnecte(authentication).getId();
        model.addAttribute("demandes", demandeService.listerPourAuteur(auteurId));
        return "employe/demandes-liste";
    }

    @GetMapping("/nouvelle")
    public String formulaire(Authentication authentication, Model model,
            @RequestParam(required = false) TypeDemande type, @RequestParam(required = false) Long posteCibleId) {
        preparerRattachement(authentication, model);
        model.addAttribute("postes", posteRepository.findAll());
        model.addAttribute("types", TypeDemande.values());
        model.addAttribute("typeChoisi",type); model.addAttribute("posteChoisi",posteCibleId);
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
            preparerRattachement(authentication, model);
            model.addAttribute("typeChoisi", type);
            model.addAttribute("posteChoisi", posteCibleId);
            model.addAttribute("description", description);
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("postes", posteRepository.findAll());
            model.addAttribute("types", TypeDemande.values());
            return "employe/demande-form";
        }
    }

    private void preparerRattachement(Authentication authentication, Model model) {
        var utilisateur = utilisateurConnecte(authentication);
        affectationAutomatique.completer(utilisateur);
        String blocage = null;
        if (utilisateur.getService() == null) {
            blocage = "Votre compte n’est pas encore rattaché à un service. Contactez les ressources humaines pour renseigner votre service et son responsable avant de déposer une demande.";
        } else if (utilisateur.getService().getChefService() == null) {
            blocage = "Votre service n’a pas encore de responsable désigné. Contactez les ressources humaines pour compléter le circuit de validation.";
        }
        model.addAttribute("avertissementRattachement", blocage == null ? null : "Les transferts sont traités directement par les RH. Pour les autres demandes, votre service et son responsable doivent être renseignés.");
    }

    private com.sirh.utilisateur.entity.Utilisateur utilisateurConnecte(Authentication authentication) {
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }
}
