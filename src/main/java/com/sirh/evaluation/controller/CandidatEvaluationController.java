package com.sirh.evaluation.controller;

import com.sirh.evaluation.entity.Evaluation;
import com.sirh.evaluation.service.EvaluationService;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.service.CandidatureService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDAT')")
public class CandidatEvaluationController {

    private static final String PREFIXE_CHOIX = "choix_";
    private static final String PREFIXE_TEXTE = "texte_";

    private final EvaluationService evaluationService;
    private final CandidatureService candidatureService;

    @GetMapping("/candidat/candidatures/{candidatureId}/evaluations")
    public String liste(@PathVariable Long candidatureId, Authentication authentication, Model model) {
        Candidature candidature = candidatureService.obtenir(candidatureId);
        if (!candidature.getCandidat().getUtilisateur().getEmail().equals(authentication.getName())) {
            throw new AccessDeniedException("Cette candidature ne vous appartient pas.");
        }
        model.addAttribute("candidature", candidature);
        model.addAttribute("evaluations", evaluationService.listerPourCandidature(candidatureId));
        return "candidat/evaluations-liste";
    }

    @GetMapping("/candidat/evaluations/{id}/repondre")
    public String formulaire(@PathVariable Long id, Authentication authentication, Model model) {
        Evaluation evaluation = verifierProprietaire(id, authentication);
        model.addAttribute("evaluation", evaluation);
        model.addAttribute("questions", evaluationService.questionsDe(id));
        return "candidat/evaluation-repondre";
    }

    @PostMapping("/candidat/evaluations/{id}/repondre")
    public String soumettre(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        verifierProprietaire(id, authentication);
        Map<Long, Long> reponsesQcm = new HashMap<>();
        Map<Long, String> reponsesTexte = new HashMap<>();
        request.getParameterMap().forEach((nom, valeurs) -> {
            if (nom.startsWith(PREFIXE_CHOIX) && valeurs.length > 0 && !valeurs[0].isBlank()) {
                reponsesQcm.put(Long.valueOf(nom.substring(PREFIXE_CHOIX.length())), Long.valueOf(valeurs[0]));
            } else if (nom.startsWith(PREFIXE_TEXTE)) {
                reponsesTexte.put(Long.valueOf(nom.substring(PREFIXE_TEXTE.length())), valeurs[0]);
            }
        });
        evaluationService.soumettre(id, reponsesQcm, reponsesTexte);
        return "redirect:/candidat/mes-candidatures";
    }

    private Evaluation verifierProprietaire(Long evaluationId, Authentication authentication) {
        Evaluation evaluation = evaluationService.obtenir(evaluationId);
        if (!evaluation.getCandidature().getCandidat().getUtilisateur().getEmail().equals(authentication.getName())) {
            throw new AccessDeniedException("Cette évaluation ne vous appartient pas.");
        }
        return evaluation;
    }
}
