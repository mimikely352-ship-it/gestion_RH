package com.sirh.embauche.controller;

import com.sirh.commun.enums.ActionAudit;
import com.sirh.commun.enums.Role;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.embauche.service.AuditDossierService;
import com.sirh.embauche.service.FicheEmployeService;
import com.sirh.embauche.service.HabilitationService;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Consultation du dossier salarié par les autres rôles, selon leurs habilitations :
 * DG (tout), Direction et chef de service (situation professionnelle), employé (son propre dossier).
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
        model.addAttribute("fiche", fiches.obtenir(dossierId, h));
        audit.journaliser(dossierId, compte, ActionAudit.CONSULTATION, null);
        EffectifController.preparerFormulaires(model);
        return "rh/employe-fiche";
    }
}
