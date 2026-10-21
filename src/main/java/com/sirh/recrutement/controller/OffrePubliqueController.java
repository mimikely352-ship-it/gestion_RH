package com.sirh.recrutement.controller;

import com.sirh.recrutement.service.OffreEmploiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class OffrePubliqueController {

    private final OffreEmploiService offreEmploiService;

    @GetMapping("/")
    public String accueil() {
        return "redirect:/offres";
    }

    @GetMapping("/offres")
    public String liste(Model model) {
        model.addAttribute("offres", offreEmploiService.listerPubliees());
        return "public/offres-liste";
    }

    @GetMapping("/offres/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("offre", offreEmploiService.obtenir(id));
        return "public/offre-detail";
    }
}
