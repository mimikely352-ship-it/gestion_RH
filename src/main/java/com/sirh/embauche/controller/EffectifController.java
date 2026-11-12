package com.sirh.embauche.controller;

import com.sirh.commun.enums.ActionAudit;
import com.sirh.commun.enums.MotifLicenciement;
import com.sirh.embauche.service.AuditDossierService;
import com.sirh.embauche.service.HabilitationService;
import com.sirh.commun.enums.TypeContrat;
import com.sirh.commun.enums.TypeDepart;
import com.sirh.embauche.service.DepartService;
import com.sirh.embauche.service.EffectifService;
import com.sirh.embauche.service.FicheEmployeService;
import com.sirh.embauche.service.OnboardingService;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final FicheEmployeService fiches;
    private final OnboardingService onboarding;
    private final DepartService departs;
    private final HabilitationService habilitations;
    private final AuditDossierService audit;
    private final com.sirh.embauche.service.ContratService contrats;

    @GetMapping("/rh/employes")
    public String liste(@RequestParam(defaultValue = "0") int page, Model model) {
        var employes = effectif.lister(page);
        model.addAttribute("employes", employes);
        model.addAttribute("avancementIntegration", onboarding.avancement(employes.stream()
                .map(EffectifService.Employe::getDossierId).filter(java.util.Objects::nonNull).toList()));
        model.addAttribute("departs", departs.recents());
        model.addAttribute("documentsARenouveler", fiches.documentsARenouveler());
        model.addAttribute("contratsAEcheance", contrats.aEcheance());
        model.addAttribute("nombreEmployes", effectif.compter());
        model.addAttribute("pageEffectif", Math.max(0, page));
        model.addAttribute("postes", postes.findAll());
        return "rh/employes";
    }

    @GetMapping("/rh/employes/{dossierId:\\d+}")
    public String fiche(@PathVariable Long dossierId, Model model, org.springframework.security.core.Authentication compte) {
        var h = habilitations.pour(compte, fiches.dossier(dossierId));
        model.addAttribute("fiche", fiches.obtenir(dossierId, h));
        audit.journaliser(dossierId, compte, ActionAudit.CONSULTATION, null);
        preparerFormulaires(model);
        model.addAttribute("postes", postes.findAll());
        return "rh/employe-fiche";
    }

    /** Listes de valeurs des formulaires de la fiche (partagées avec la vue en lecture seule). */
    public static void preparerFormulaires(Model model) {
        model.addAttribute("typesContrat", TypeContrat.values());
        model.addAttribute("typesDepart", TypeDepart.values());
        model.addAttribute("motifsLicenciement", MotifLicenciement.values());
        model.addAttribute("typesDocument", com.sirh.commun.enums.TypeDocumentAdministratif.values());
        model.addAttribute("sexes", com.sirh.embauche.service.DossierSalarieService.SEXES);
        model.addAttribute("situationsFamiliales", com.sirh.embauche.service.DossierSalarieService.SITUATIONS_FAMILIALES);
        model.addAttribute("aujourdhui", java.time.LocalDate.now().toString());
    }

    @PostMapping("/rh/employes/date-entree")
    public String dateEntree(@RequestParam Long dossierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateEntree,
            @RequestParam(defaultValue = "false") boolean depuisFiche,
            RedirectAttributes flash, org.springframework.security.core.Authentication authentication) {
        try {
            effectif.renseignerEntree(dossierId, dateEntree, authentication.getName());
            audit.journaliser(dossierId, authentication, ActionAudit.DATE_ENTREE_MODIFIEE, "Nouvelle date : " + dateEntree);
            flash.addFlashAttribute("message", "Date d’entrée enregistrée.");
        } catch (com.sirh.commun.exception.BusinessException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return depuisFiche ? "redirect:/rh/employes/" + dossierId : "redirect:/rh/employes";
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
            @RequestParam(defaultValue = "false") boolean depuisFiche,
            RedirectAttributes flash, org.springframework.security.core.Authentication authentication) {
        try {
            parcours.promouvoirDirectement(dossierId, posteCibleId, dateEffet);
            audit.journaliser(dossierId, authentication, ActionAudit.PROMOTION, "Poste cible n°" + posteCibleId);
            flash.addFlashAttribute("message", "Employé promu/muté avec effet immédiat.");
        } catch (com.sirh.commun.exception.BusinessException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return depuisFiche ? "redirect:/rh/employes/" + dossierId : "redirect:/rh/employes";
    }
}
