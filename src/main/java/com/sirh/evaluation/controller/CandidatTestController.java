package com.sirh.evaluation.controller;

import com.sirh.evaluation.entity.Test;
import com.sirh.evaluation.service.TestService;
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
public class CandidatTestController {

    private static final String PREFIXE_REPONSE = "reponse_";

    private final TestService testService;
    private final CandidatureService candidatureService;
    private final com.sirh.evaluation.service.PassageTestService passageTestService;

    @GetMapping("/candidat/candidatures/{candidatureId}/tests")
    public String liste(@PathVariable Long candidatureId, Authentication authentication, Model model) {
        Candidature candidature = verifierProprietaire(candidatureId, authentication);
        model.addAttribute("candidature", candidature);
        model.addAttribute("tests", testService.listerPourOffre(candidature.getOffre().getId()));
        model.addAttribute("passages", passageTestService.listerPourCandidature(candidatureId));
        return "candidat/tests-liste";
    }

    @GetMapping("/candidat/tests/{testId}/passer")
    public String formulaire(@PathVariable Long testId, @RequestParam Long candidatureId,
                              Authentication authentication, Model model) {
        verifierProprietaire(candidatureId, authentication);
        Test test = testService.obtenir(testId);
        model.addAttribute("test", test);
        model.addAttribute("candidatureId", candidatureId);
        model.addAttribute("questions", testService.questionsDe(testId));
        return "candidat/test-passer";
    }

    @PostMapping("/candidat/tests/{testId}/passer")
    public String soumettre(@PathVariable Long testId, @RequestParam Long candidatureId,
                             Authentication authentication, HttpServletRequest request) {
        verifierProprietaire(candidatureId, authentication);
        Map<Long, Long> reponses = new HashMap<>();
        request.getParameterMap().forEach((nom, valeurs) -> {
            if (nom.startsWith(PREFIXE_REPONSE) && valeurs.length > 0 && !valeurs[0].isBlank()) {
                Long questionId = Long.valueOf(nom.substring(PREFIXE_REPONSE.length()));
                reponses.put(questionId, Long.valueOf(valeurs[0]));
            }
        });
        passageTestService.soumettre(candidatureId, testId, reponses);
        return "redirect:/candidat/candidatures/" + candidatureId + "/tests";
    }

    private Candidature verifierProprietaire(Long candidatureId, Authentication authentication) {
        Candidature candidature = candidatureService.obtenir(candidatureId);
        if (!candidature.getCandidat().getUtilisateur().getEmail().equals(authentication.getName())) {
            throw new AccessDeniedException("Cette candidature ne vous appartient pas.");
        }
        return candidature;
    }
}
