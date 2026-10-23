package com.sirh.utilisateur.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Compatibilité des anciens liens : aucune modification du compte hors d'une affectation. */
@Controller @RequestMapping("/rh/employes/rattachements") @PreAuthorize("hasRole('RH')")
public class RhRattachementController {
    @GetMapping
    public String liste() { return "redirect:/rh/employes"; }

    @PostMapping
    public String ancienFormulaire(RedirectAttributes flash) {
        flash.addFlashAttribute("message", "Le service est maintenant issu du dossier employé. Utilisez l’embauche ou un mouvement interne pour gérer une affectation.");
        return "redirect:/rh/employes";
    }
}
