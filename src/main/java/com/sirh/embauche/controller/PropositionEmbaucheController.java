package com.sirh.embauche.controller;

import com.sirh.commun.enums.Role;
import com.sirh.commun.enums.TypeContrat;
import com.sirh.commun.exception.BusinessException;
import com.sirh.embauche.service.PropositionEmbaucheService;
import com.sirh.utilisateur.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class PropositionEmbaucheController {
    private final PropositionEmbaucheService propositions;
    private final AuthService authService;

    @PostMapping("/rh/candidatures/{id}/proposition")
    @PreAuthorize("hasRole('RH')")
    public String envoyer(@PathVariable Long id, @RequestParam TypeContrat typeContrat,
            @RequestParam BigDecimal salaire,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Role role, @RequestParam(required = false) Long serviceId,
            Authentication auth, RedirectAttributes flash) {
        try {
            propositions.envoyer(id, typeContrat, salaire, debut, fin, role, serviceId, auth.getName());
            flash.addFlashAttribute("succes", "Proposition envoyée. Son acceptation par le candidat créera automatiquement le dossier salarié.");
        } catch (BusinessException e) { flash.addFlashAttribute("erreur", e.getMessage()); }
        return "redirect:/rh/candidatures/" + id;
    }

    @GetMapping("/candidat/propositions/{id}")
    @PreAuthorize("hasRole('CANDIDAT')")
    public String consulter(@PathVariable Long id, Authentication auth, Model model) {
        model.addAttribute("candidature", propositions.consulter(id, auth.getName()));
        return "candidat/proposition";
    }

    @PostMapping("/candidat/propositions/{id}/reponse")
    @PreAuthorize("hasRole('CANDIDAT')")
    public String repondre(@PathVariable Long id, @RequestParam boolean accepter, Authentication auth,
                           HttpServletResponse response, RedirectAttributes flash) {
        try {
            var reponse = propositions.repondre(id, auth.getName(), accepter);
            if (reponse.embauche()) {
                // Le compte est désormais interne : la session est renouvelée avec son nouveau rôle.
                authService.renouvelerSession(response, reponse.utilisateur());
                flash.addFlashAttribute("message", "Bienvenue ! Votre embauche est confirmée : votre dossier salarié "
                        + reponse.dossier().getMatricule() + " est créé. Retrouvez ici votre contrat et votre intégration.");
                return "redirect:/dossiers/" + reponse.dossier().getId();
            }
            flash.addFlashAttribute("succes", "Votre refus a été transmis aux RH.");
        } catch (BusinessException e) {
            flash.addFlashAttribute("erreur", "Votre réponse n’a pas pu être enregistrée : " + e.getMessage());
        }
        return "redirect:/candidat/propositions/" + id;
    }
}
