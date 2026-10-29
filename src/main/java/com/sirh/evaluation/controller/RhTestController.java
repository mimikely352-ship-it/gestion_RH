package com.sirh.evaluation.controller;

import com.sirh.commun.enums.DecisionTest;
import com.sirh.evaluation.entity.PassageTest;
import com.sirh.evaluation.entity.Test;
import com.sirh.evaluation.service.PassageTestService;
import com.sirh.evaluation.service.TestService;
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
public class RhTestController {

    private final TestService testService;
    private final PassageTestService passageTestService;

    @GetMapping("/rh/offres/{offreId}/tests")
    public String liste(@PathVariable Long offreId, Model model) {
        model.addAttribute("offreId", offreId);
        model.addAttribute("tests", testService.listerPourOffre(offreId));
        return "rh/tests-liste";
    }

    @PostMapping("/rh/offres/{offreId}/tests")
    public String creer(@PathVariable Long offreId, @RequestParam int niveau, @RequestParam String titre) {
        Test test = testService.creer(offreId, niveau, titre, List.of());
        return "redirect:/rh/tests/" + test.getId();
    }

    @GetMapping("/rh/tests/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Test test = testService.obtenir(id);
        model.addAttribute("test", test);
        model.addAttribute("questions", testService.questionsDe(id));
        List<com.sirh.evaluation.entity.PassageTest> passages = passageTestService.listerPourTest(id);
        model.addAttribute("passages", passages);
        java.util.Map<Long, java.util.List<com.sirh.evaluation.entity.ReponseTest>> reponsesParPassage = new java.util.HashMap<>();
        for (var passage : passages) {
            reponsesParPassage.put(passage.getId(), passageTestService.reponsesDe(passage.getId()));
        }
        model.addAttribute("reponsesParPassage", reponsesParPassage);
        return "rh/test-detail";
    }

    @PostMapping("/rh/tests/{id}/questions")
    public String ajouterQuestion(@PathVariable Long id,
                                   @RequestParam String enonce,
                                   @RequestParam(required = false) String choix1,
                                   @RequestParam(required = false) String choix2,
                                   @RequestParam(required = false) String choix3,
                                   @RequestParam(required = false) String choix4,
                                   @RequestParam int correct) {
        List<String> libelles = java.util.Arrays.asList(choix1, choix2, choix3, choix4);
        List<TestService.ChoixSaisi> choix = new ArrayList<>();
        for (int i = 0; i < libelles.size(); i++) {
            if (libelles.get(i) != null && !libelles.get(i).isBlank()) {
                choix.add(new TestService.ChoixSaisi(libelles.get(i), (i + 1) == correct));
            }
        }
        testService.ajouterQuestion(id, enonce, choix);
        return "redirect:/rh/tests/" + id;
    }

    @PostMapping("/rh/passages/{id}/decider")
    public String deciderPassage(@PathVariable Long id, @RequestParam DecisionTest decision) {
        PassageTest passage = passageTestService.decider(id, decision);
        return "redirect:/rh/candidatures/" + passage.getCandidature().getId();
    }
}
