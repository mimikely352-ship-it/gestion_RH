package com.sirh.embauche.controller;

import com.sirh.embauche.service.EffectifService;
import com.sirh.embauche.service.ParcoursEmployeService;
import com.sirh.organisation.repository.PosteRepository;
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
    private final ParcoursEmployeService parcours;
    private final PosteRepository postes;

    @GetMapping("/rh/employes")
    public String liste(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("employes", effectif.lister(page));
        model.addAttribute("nombreEmployes", effectif.compter());
        model.addAttribute("pageEffectif", Math.max(0, page));
        model.addAttribute("postes", postes.findAll());
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

    @GetMapping("/rh/employes/nouveau")
    public String formulaireNouveau(Model model) {
        model.addAttribute("postes", postes.findAll());
        return "rh/employe-nouveau";
    }

    @PostMapping("/rh/employes/nouveau")
    public String creer(@RequestParam String email, @RequestParam String motDePasse,
            @RequestParam String nom, @RequestParam String prenom, @RequestParam Long posteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateEntree,
            Model model, RedirectAttributes flash) {
        try {
            parcours.embaucherDirectement(email, motDePasse, nom, prenom, posteId, dateEntree);
            flash.addFlashAttribute("message", "Compte employé créé.");
            return "redirect:/rh/employes";
        } catch (com.sirh.commun.exception.BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("postes", postes.findAll());
            return "rh/employe-nouveau";
        }
    }

    @PostMapping("/rh/employes/{dossierId}/promouvoir")
    public String promouvoir(@org.springframework.web.bind.annotation.PathVariable Long dossierId,
            @RequestParam Long posteCibleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateEffet,
            RedirectAttributes flash) {
        try {
            parcours.promouvoirDirectement(dossierId, posteCibleId, dateEffet);
            flash.addFlashAttribute("message", "Employé promu/muté avec effet immédiat.");
        } catch (com.sirh.commun.exception.BusinessException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/rh/employes";
    }
}
