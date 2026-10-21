package com.sirh.recrutement.controller;

import com.sirh.commun.enums.Decision;
import com.sirh.recrutement.entity.Entretien;
import com.sirh.recrutement.service.EntretienService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/rh/entretiens")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RH')")
public class RhEntretienController {

    private final EntretienService entretienService;

    @PostMapping("/{id}/decider")
    public String decider(@PathVariable Long id, @RequestParam Decision decision,
                           @RequestParam(required = false) String commentaire) {
        Entretien entretien = entretienService.decider(id, decision, commentaire);
        return "redirect:/rh/candidatures/" + entretien.getCandidature().getId();
    }
}
