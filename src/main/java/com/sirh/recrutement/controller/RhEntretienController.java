package com.sirh.recrutement.controller;

import com.sirh.commun.enums.Decision;
import com.sirh.recrutement.entity.Entretien;
import com.sirh.recrutement.repository.EntretienRepository;
import com.sirh.recrutement.service.EntretienService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final EntretienRepository entretienRepository;

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("entretiens", entretienRepository.findAll());
        return "rh/entretiens-liste";
    }

    /** {@code suite} (ENTRETIEN_SUIVANT, TESTS, REFUS) prime ; {@code decision} reste accepté pour l'API historique. */
    @PostMapping("/{id}/decider")
    public String decider(@PathVariable Long id,
                           @RequestParam(required = false) EntretienService.Suite suite,
                           @RequestParam(required = false) Decision decision,
                           @RequestParam(required = false) String commentaire,
                           org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        Entretien entretien = entretienRepository.findById(id)
                .orElseThrow(() -> new com.sirh.commun.exception.ResourceNotFoundException("Entretien introuvable : " + id));
        Long candidatureId = entretien.getCandidature().getId();
        try {
            if (suite != null) entretienService.decider(id, suite, commentaire);
            else entretienService.decider(id, decision, commentaire);
            flash.addFlashAttribute("succes", suite == EntretienService.Suite.ENTRETIEN_SUIVANT
                    ? "Décision enregistrée. Planifiez l’entretien suivant." : "Décision d’entretien enregistrée.");
        } catch (com.sirh.commun.exception.BusinessException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/rh/candidatures/" + candidatureId;
    }
}
