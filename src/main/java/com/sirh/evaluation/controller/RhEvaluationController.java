package com.sirh.evaluation.controller;

import com.sirh.commun.enums.Decision;
import com.sirh.commun.enums.FormatEvaluation;
import com.sirh.evaluation.entity.Evaluation;
import com.sirh.evaluation.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('RH')")
public class RhEvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping("/rh/candidatures/{candidatureId}/evaluations")
    public String creer(@PathVariable Long candidatureId, @RequestParam String titre,
                         @RequestParam FormatEvaluation format) {
        Evaluation evaluation = evaluationService.creer(candidatureId, titre, format, List.of());
        return "redirect:/rh/evaluations/" + evaluation.getId();
    }

    @GetMapping("/rh/evaluations/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Evaluation evaluation = evaluationService.obtenir(id);
        model.addAttribute("evaluation", evaluation);
        model.addAttribute("questions", evaluationService.questionsDe(id));
        model.addAttribute("reponses", evaluationService.reponsesDe(id));
        return "rh/evaluation-detail";
    }

    @PostMapping("/rh/evaluations/{id}/questions")
    public String ajouterQuestion(@PathVariable Long id,
                                   @RequestParam String enonce,
                                   @RequestParam FormatEvaluation type,
                                   @RequestParam(required = false) String choix1,
                                   @RequestParam(required = false) String choix2,
                                   @RequestParam(required = false) String choix3,
                                   @RequestParam(required = false) String choix4,
                                   @RequestParam(required = false) Integer correct) {
        List<EvaluationService.ChoixSaisi> choix = new ArrayList<>();
        List<String> libelles = List.of(choix1 == null ? "" : choix1, choix2 == null ? "" : choix2,
                choix3 == null ? "" : choix3, choix4 == null ? "" : choix4);
        for (int i = 0; i < libelles.size(); i++) {
            if (!libelles.get(i).isBlank()) {
                choix.add(new EvaluationService.ChoixSaisi(libelles.get(i), correct != null && (i + 1) == correct));
            }
        }
        evaluationService.ajouterQuestion(id, enonce, type, choix);
        return "redirect:/rh/evaluations/" + id;
    }

    @PostMapping("/rh/evaluations/{id}/decider")
    public String decider(@PathVariable Long id, @RequestParam Decision decision) {
        Evaluation evaluation = evaluationService.decider(id, decision);
        return "redirect:/rh/candidatures/" + evaluation.getCandidature().getId();
    }
}
