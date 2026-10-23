package com.sirh.candidat.controller;

import com.sirh.candidat.entity.Candidat;
import com.sirh.candidat.repository.CandidatRepository;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.service.CandidatureService;
import com.sirh.recrutement.service.EntretienService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CANDIDAT','EMPLOYE')")
public class CandidatCandidatureController {

    private final CandidatureService candidatureService;
    private final CandidatRepository candidatRepository;
    private final EntretienService entretienService;
    private final com.sirh.candidat.service.DossierCandidatureService dossierCandidatureService;

    @PreAuthorize("hasRole('CANDIDAT')")
    @PostMapping("/candidat/candidatures")
    public String postuler(@RequestParam Long offreId, Authentication authentication,
            @RequestParam(required = false) Map<String, org.springframework.web.multipart.MultipartFile> fichiers,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirection) {
        Candidat candidat = candidatCourant(authentication);
        try {
            dossierCandidatureService.deposer(candidat.getId(), offreId, fichiers == null ? Map.of() : fichiers);
            return "redirect:/candidat/mes-candidatures";
        } catch (BusinessException e) {
            redirection.addFlashAttribute("erreur", e.getMessage() + " Veuillez sélectionner à nouveau les fichiers.");
            return "redirect:/offres/" + offreId;
        }
    }

    @GetMapping("/candidat/mes-candidatures")
    public String mesCandidatures(Authentication authentication, Model model) {
        Candidat candidat = candidatCourant(authentication);
        List<Candidature> candidatures = candidatureService.listerPourCandidat(candidat.getId());
        Map<Long, Object> entretiensParCandidature = new HashMap<>();
        for (Candidature c : candidatures) {
            entretiensParCandidature.put(c.getId(), entretienService.listerPourCandidature(c.getId()));
        }
        model.addAttribute("candidat", candidat);
        model.addAttribute("candidatures", candidatures);
        model.addAttribute("entretiensParCandidature", entretiensParCandidature);
        return "candidat/mes-candidatures";
    }

    private Candidat candidatCourant(Authentication authentication) {
        return candidatRepository.findByUtilisateurEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Profil candidat introuvable."));
    }
}
