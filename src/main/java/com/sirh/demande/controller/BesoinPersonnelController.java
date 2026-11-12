package com.sirh.demande.controller;

import com.sirh.commun.enums.*;
import com.sirh.commun.exception.*;
import com.sirh.demande.service.DemandeService;
import com.sirh.organisation.repository.PosteRepository;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/besoins")
@PreAuthorize("hasRole('CHEF_SERVICE')")
@RequiredArgsConstructor
public class BesoinPersonnelController {
    private final DemandeService demandes;
    private final PosteRepository postes;
    private final UtilisateurRepository utilisateurs;

    @GetMapping
    public String liste(Authentication auth, Model model) {
        model.addAttribute("demandes", demandes.listerPourAuteur(auteur(auth)).stream()
                .filter(d -> d.getType() == TypeDemande.RECRUTEMENT).toList());
        return "chef-service/besoins-liste";
    }
    @GetMapping("/nouveau")
    public String formulaire(Model model) { preparer(model); return "employe/demande-form"; }

    @PostMapping
    public String creer(Authentication auth, @RequestParam Long posteCibleId, @RequestParam String description,
            @RequestParam(required = false) Integer effectifDemande,
            @RequestParam(required = false) BigDecimal salaireMensuelParPoste,
            @RequestParam(required = false) BigDecimal budgetGlobalRecrutement,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateSouhaitee,
            @RequestParam(required = false) MotifBesoin motifBesoin, Model model) {
        try {
            demandes.creer(auteur(auth), TypeDemande.RECRUTEMENT, posteCibleId, description,
                    effectifDemande, salaireMensuelParPoste, budgetGlobalRecrutement, dateSouhaitee, motifBesoin);
            return "redirect:/besoins";
        } catch (BusinessException e) {
            preparer(model);
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("posteChoisi", posteCibleId);
            model.addAttribute("description", description);
            model.addAttribute("effectifDemande", effectifDemande);
            model.addAttribute("salaireMensuelParPoste", salaireMensuelParPoste);
            model.addAttribute("budgetGlobalRecrutement", budgetGlobalRecrutement);
            model.addAttribute("dateSouhaitee", dateSouhaitee);
            model.addAttribute("motifBesoin", motifBesoin);
            return "employe/demande-form";
        }
    }
    private Long auteur(Authentication auth) {
        return utilisateurs.findByEmail(auth.getName()).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable.")).getId();
    }
    private void preparer(Model model) {
        model.addAttribute("postes", postes.findAll());
        model.addAttribute("types", java.util.List.of(TypeDemande.RECRUTEMENT));
        model.addAttribute("typeChoisi", TypeDemande.RECRUTEMENT);
        model.addAttribute("actionDemande", "/besoins");
    }
}
