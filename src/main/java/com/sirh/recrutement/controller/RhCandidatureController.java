package com.sirh.recrutement.controller;

import com.sirh.candidat.repository.DocumentRepository;
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
    private final DocumentRepository documentRepository;
    private final EmbaucheService embaucheService;

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
        model.addAttribute("cv", documentRepository.findFirstByCandidatIdAndTypeOrderByDateUploadDesc(candidature.getCandidat().getId(), "CV").orElse(null));
        return "rh/candidature-detail";
    }

    @PostMapping("/{id}/embauche")
    public String finaliser(@PathVariable Long id, @RequestParam Role role) {
        embaucheService.finaliser(id, role, null);
        return "redirect:/rh/candidatures/" + id;
    }

    @PostMapping("/{id}/entretiens")
    public String planifierEntretien(@PathVariable Long id,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateHeure,
                                      @RequestParam(required = false) String lieu) {
        entretienService.planifier(id, dateHeure, lieu);
        return "redirect:/rh/candidatures/" + id;
    }
}
