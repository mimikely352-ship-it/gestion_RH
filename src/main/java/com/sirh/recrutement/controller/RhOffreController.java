package com.sirh.recrutement.controller;

import com.sirh.organisation.repository.PosteRepository;
import com.sirh.recrutement.service.CandidatureService;
import com.sirh.recrutement.service.OffreEmploiService;
import com.sirh.referentiel.repository.DiplomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

@Controller
@RequestMapping("/rh/offres")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RH')")
public class RhOffreController {

    private final OffreEmploiService offreEmploiService;
    private final CandidatureService candidatureService;
    private final PosteRepository posteRepository;
    private final DiplomeRepository diplomeRepository;

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("offres", offreEmploiService.listerToutes());
        return "rh/offres-liste";
    }

    @GetMapping("/nouvelle")
    public String formulaireCreation(@RequestParam(required = false) Long demandeId, Model model) {
        model.addAttribute("postes", posteRepository.findAll());
        model.addAttribute("diplomes", diplomeRepository.findAll());
        model.addAttribute("demandeId", demandeId);
        return "rh/offre-form";
    }

    @PostMapping
    public String creer(@RequestParam Long posteId,
                         @RequestParam String titre,
                         @RequestParam String description,
                         @RequestParam int nbPostes,
                         @RequestParam(required = false) Integer experienceMinMois,
                         @RequestParam(defaultValue = "60") int seuilAcceptation,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateLimite,
                         @RequestParam(required = false) Set<Long> diplomeIds,
                         @RequestParam(required = false) Long demandeId) {
        var offre = offreEmploiService.creer(posteId, titre, description, nbPostes, experienceMinMois,
                seuilAcceptation, dateLimite, diplomeIds == null ? Set.of() : diplomeIds, demandeId);
        return "redirect:/rh/offres/" + offre.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("offre", offreEmploiService.obtenir(id));
        model.addAttribute("candidatures", candidatureService.listerPourOffre(id));
        return "rh/offre-detail";
    }

    @PostMapping("/{id}/publier")
    public String publier(@PathVariable Long id) {
        offreEmploiService.publier(id);
        return "redirect:/rh/offres/" + id;
    }

    @PostMapping("/{id}/cloturer")
    public String cloturer(@PathVariable Long id) {
        offreEmploiService.cloturer(id);
        return "redirect:/rh/offres/" + id;
    }
}
