package com.sirh.embauche.controller;

import com.sirh.embauche.service.EffectifService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('RH')")
public class EffectifController {
    private final EffectifService effectif;

    @GetMapping("/rh/employes")
    public String liste(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("employes", effectif.lister(page));
        model.addAttribute("nombreEmployes", effectif.compter());
        model.addAttribute("pageEffectif", Math.max(0, page));
        return "rh/employes";
    }

    @PostMapping("/rh/employes/date-entree")
    public String dateEntree(@RequestParam Long dossierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateEntree,
            RedirectAttributes flash, org.springframework.security.core.Authentication authentication) {
        try {
            effectif.renseignerEntree(dossierId, dateEntree, authentication.getName());
            flash.addFlashAttribute("message", "Date d’entrée enregistrée.");
        } catch (com.sirh.commun.exception.BusinessException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/rh/employes";
    }
}
