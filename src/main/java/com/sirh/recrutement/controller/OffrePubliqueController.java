package com.sirh.recrutement.controller;

import com.sirh.recrutement.service.OffreEmploiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class OffrePubliqueController {

    private final OffreEmploiService offreEmploiService;

    @GetMapping("/")
    public String accueil() {
        return "redirect:/offres";
    }

    @GetMapping("/offres")
    public String liste(@RequestParam(defaultValue = "") String q, Model model) {
        String recherche = normaliser(q.trim());
        var offres = offreEmploiService.listerPubliees().stream()
                .filter(o -> recherche.isEmpty() || normaliser(o.getTitre() + " " + o.getPosteIntitule() + " " + o.getEntrepriseNom()).contains(recherche))
                .toList();
        model.addAttribute("offres", offres);
        model.addAttribute("rechercheOffres", q.trim());
        return "public/offres-liste";
    }

    private String normaliser(String texte) {
        return java.text.Normalizer.normalize(texte, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.FRENCH);
    }

    @GetMapping("/offres/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var offre = offreEmploiService.obtenirPublique(id);
        model.addAttribute("offre", offre);
        model.addAttribute("texteAnnonce", com.sirh.recrutement.dto.TexteAnnonce.separer(offre.getDescription()));
        model.addAttribute("candidaturesOuvertes", offre.getStatut() == com.sirh.commun.enums.StatutOffre.PUBLIEE
                && (offre.getDateLimite() == null || !offre.getDateLimite().isBefore(java.time.LocalDate.now())));
        return "public/offre-detail";
    }
}
