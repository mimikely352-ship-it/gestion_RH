package com.sirh.embauche.controller;

import com.sirh.commun.enums.ActionAudit;
import com.sirh.commun.enums.Role;
import com.sirh.commun.enums.TacheOnboarding;
import com.sirh.commun.exception.BusinessException;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.embauche.service.AuditDossierService;
import com.sirh.embauche.service.FicheEmployeService;
import com.sirh.embauche.service.HabilitationService;
import com.sirh.embauche.service.OnboardingService;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

/**
 * Consultation du dossier salarié par les autres rôles, selon leurs habilitations :
 * DG (tout), Direction et chef de service (situation professionnelle), employé (son propre dossier).
 * Le chef du service de l'employé coche en outre les tâches d'intégration qui lui sont confiées.
 * Les RH utilisent /rh/employes, qui permet aussi la modification.
 */
@Controller
@RequiredArgsConstructor
public class DossierSalarieController {
    private final DossierEmployeRepository dossiers;
    private final UtilisateurRepository utilisateurs;
    private final FicheEmployeService fiches;
    private final HabilitationService habilitations;
    private final AuditDossierService audit;
    private final OnboardingService onboarding;

    @GetMapping("/dossiers")
    @Transactional(readOnly = true)
    public String liste(Authentication compte, Model model) {
        if (HabilitationService.estRole(compte, Role.RH)) return "redirect:/rh/employes";
        var lecteur = utilisateurs.findByEmail(compte.getName()).orElseThrow(() -> new AccessDeniedException("Compte inconnu."));
        var visibles = dossiers.findByUtilisateurActifTrueOrderByNomAscPrenomAsc().stream()
                .filter(d -> !d.getUtilisateur().getId().equals(lecteur.getId()))
                .filter(d -> HabilitationService.pour(lecteur, d).lecture())
                .map(d -> java.util.Map.of("id", d.getId(), "matricule", d.getMatricule() == null ? "" : d.getMatricule(),
                        "nom", d.getPrenom() + " " + d.getNom(),
                        "service", d.getUtilisateur().getService() == null ? "Service non renseigné" : d.getUtilisateur().getService().getNom()))
                .toList();
        model.addAttribute("dossiers", visibles);
        model.addAttribute("perimetre", lecteur.getRole() == Role.CHEF_SERVICE ? "les employés de votre service" : "tous les salariés en poste");
        return "commun/dossiers-liste";
    }

    @GetMapping("/mon-dossier")
    public String monDossier(Authentication compte, Model model) {
        var lecteur = utilisateurs.findByEmail(compte.getName()).orElseThrow(() -> new AccessDeniedException("Compte inconnu."));
        var dossier = dossiers.findByUtilisateurId(lecteur.getId()).orElse(null);
        if (dossier != null) return "redirect:/dossiers/" + dossier.getId();
        model.addAttribute("aucunDossier", true);
        model.addAttribute("dossiers", java.util.List.of());
        return "commun/dossiers-liste";
    }

    @GetMapping("/dossiers/{dossierId:\\d+}")
    public String consulter(@PathVariable Long dossierId, Authentication compte, Model model) {
        if (HabilitationService.estRole(compte, Role.RH)) return "redirect:/rh/employes/" + dossierId;
        var h = habilitations.pour(compte, fiches.dossier(dossierId));
        if (!h.lecture()) throw new AccessDeniedException("Vous n’êtes pas habilité à consulter ce dossier.");
        var fiche = fiches.obtenir(dossierId, h);
        model.addAttribute("fiche", fiche);
        model.addAttribute("peutCocherManager", estChefDuService(compte, h) && fiche.isActif());
        audit.journaliser(dossierId, compte, ActionAudit.CONSULTATION, null);
        EffectifController.preparerFormulaires(model);
        return "rh/employe-fiche";
    }

    /** Tâches d'intégration du « Manager » (matériel, formation), cochées par le chef du service de l'employé. */
    @PostMapping("/dossiers/{dossierId:\\d+}/onboarding")
    @PreAuthorize("hasRole('CHEF_SERVICE')")
    public String onboardingManager(@PathVariable Long dossierId,
            @RequestParam(name = "taches", required = false) Set<TacheOnboarding> taches,
            Authentication compte, RedirectAttributes flash) {
        if (!estChefDuService(compte, habilitations.pour(compte, fiches.dossier(dossierId))))
            throw new AccessDeniedException("Seul le chef du service de ce salarié peut cocher ces tâches.");
        try {
            onboarding.enregistrerParManager(dossierId, taches, compte.getName());
            var cochees = taches == null ? java.util.List.<TacheOnboarding>of()
                    : taches.stream().filter(TacheOnboarding::isManager).toList();
            audit.journaliser(dossierId, compte, ActionAudit.ONBOARDING_MODIFIE,
                    cochees.isEmpty() ? "Tâches du responsable : aucune cochée" : "Tâches du responsable cochées : " + cochees);
            flash.addFlashAttribute("message", "Tâches d’intégration enregistrées.");
        } catch (BusinessException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/dossiers/" + dossierId + "#onboarding";
    }

    /** Chef du service de l'employé (et non son propre dossier) : habilitation professionnelle d'un chef de service. */
    private static boolean estChefDuService(Authentication compte, HabilitationService.Habilitation h) {
        return HabilitationService.estRole(compte, Role.CHEF_SERVICE)
                && HabilitationService.Habilitation.PROFESSIONNELLE.equals(h);
    }
}
