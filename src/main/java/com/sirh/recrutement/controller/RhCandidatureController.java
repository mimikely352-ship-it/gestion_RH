package com.sirh.recrutement.controller;

import com.sirh.candidat.repository.ExperienceRepository;
import com.sirh.commun.enums.Role;
import com.sirh.embauche.service.EmbaucheService;
import com.sirh.evaluation.service.EvaluationService;
import com.sirh.evaluation.service.PassageTestService;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.repository.CandidatureRepository;
import com.sirh.recrutement.service.CandidatureService;
import com.sirh.recrutement.service.EntretienService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/rh/candidatures")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RH')")
public class RhCandidatureController {

    private final CandidatureService candidatureService;
    private final CandidatureRepository candidatureRepository;
    private final EntretienService entretienService;
    private final PassageTestService passageTestService;
    private final EvaluationService evaluationService;
    private final ExperienceRepository experienceRepository;
    private final EmbaucheService embaucheService;
    private final com.sirh.organisation.repository.ServiceRepository serviceRepository;
    private final com.sirh.evaluation.repository.TestRepository testRepository;
    private final com.sirh.embauche.service.PropositionEmbaucheService propositionEmbaucheService;
    private final com.sirh.embauche.repository.DossierEmployeRepository dossierEmployeRepository;

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("candidatures", candidatureRepository.findAll());
        return "rh/candidatures-liste";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Candidature candidature = candidatureService.obtenir(id);
        model.addAttribute("candidature", candidature);
        model.addAttribute("entretiens", entretienService.listerPourCandidature(id));
        model.addAttribute("passages", passageTestService.listerPourCandidature(id));
        model.addAttribute("evaluations", evaluationService.listerPourCandidature(id));
        model.addAttribute("experiences", experienceRepository.findByCandidatId(candidature.getCandidat().getId()));
        model.addAttribute("cv", candidature.getCvDocument());
        model.addAttribute("services", serviceRepository.findAll());
        model.addAttribute("typesContrat", com.sirh.commun.enums.TypeContrat.values());
        boolean offreAvecTests = !testRepository.findByOffreIdOrderByNiveauAsc(candidature.getOffre().getId()).isEmpty();
        model.addAttribute("offreAvecTests", offreAvecTests);
        model.addAttribute("peutPlanifierEntretien", entretienService.peutPlanifier(candidature));
        model.addAttribute("prochaineEtape", entretienService.prochaineEtape(candidature, offreAvecTests));
        model.addAttribute("budgetRestant", propositionEmbaucheService.budgetRestant(candidature));
        // Proposition d'embauche : service d'accueil et postes encore disponibles sur l'offre.
        model.addAttribute("servicePoste", com.sirh.embauche.service.AffectationAutomatiqueService
                .serviceDuPoste(candidature.getOffre().getPoste()));
        model.addAttribute("postesDisponibles", propositionEmbaucheService.postesDisponibles(candidature.getOffre()));
        if (candidature.getPropositionServiceId() != null)
            serviceRepository.findById(candidature.getPropositionServiceId())
                    .ifPresent(s -> model.addAttribute("serviceProposition", s.getNom()));
        if (candidature.getStatut() == com.sirh.commun.enums.StatutCandidature.EMBAUCHE)
            dossierEmployeRepository.findByUtilisateurId(candidature.getCandidat().getUtilisateur().getId())
                    .ifPresent(d -> model.addAttribute("dossierEmbauche", d));
        return "rh/candidature-detail";
    }

    @PostMapping("/{id}/preselection")
    public String preselection(@PathVariable Long id, @RequestParam boolean retenir) {
        candidatureService.changerStatut(id, retenir
                ? com.sirh.commun.enums.StatutCandidature.ACCEPTEE_ENTRETIEN
                : com.sirh.commun.enums.StatutCandidature.NON_RETENUE);
        return "redirect:/rh/candidatures/" + id;
    }

    /** Reprise : proposition acceptée avant l'embauche automatique, finalisée par les RH. */
    @PostMapping("/{id}/embauche")
    public String finaliser(@PathVariable Long id, @RequestParam Role role, @RequestParam(required = false) Long serviceId,
                            org.springframework.security.core.Authentication compte,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            embaucheService.finaliser(id, role, serviceId, compte.getName());
            flash.addFlashAttribute("succes", "Embauche finalisée : dossier salarié, contrat, affectation et intégration créés.");
        } catch (com.sirh.commun.exception.BusinessException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/rh/candidatures/" + id;
    }

    @PostMapping("/{id}/entretiens")
    public String planifierEntretien(@PathVariable Long id,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateHeure,
                                      @RequestParam(required = false) String lieu,
                                      org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            entretienService.planifier(id, dateHeure, lieu);
            flash.addFlashAttribute("succes", "Entretien planifié ; le candidat est notifié.");
        } catch (com.sirh.commun.exception.BusinessException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/rh/candidatures/" + id;
    }
}
