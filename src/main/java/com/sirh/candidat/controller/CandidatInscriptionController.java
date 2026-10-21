package com.sirh.candidat.controller;

import com.sirh.candidat.dto.InscriptionCandidatForm;
import com.sirh.candidat.service.CandidatInscriptionService;
import com.sirh.commun.exception.BusinessException;
import com.sirh.referentiel.repository.DiplomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/candidats/inscription")
@RequiredArgsConstructor
public class CandidatInscriptionController {

    private static final int NB_LIGNES_EXPERIENCE = 3;

    private final CandidatInscriptionService candidatInscriptionService;
    private final DiplomeRepository diplomeRepository;

    @GetMapping
    public String formulaire(Model model) {
        InscriptionCandidatForm form = new InscriptionCandidatForm();
        for (int i = 0; i < NB_LIGNES_EXPERIENCE; i++) {
            form.getExperiences().add(new InscriptionCandidatForm.ExperienceForm());
        }
        model.addAttribute("form", form);
        model.addAttribute("diplomes", diplomeRepository.findAll());
        return "public/inscription";
    }

    @PostMapping
    public String soumettre(@ModelAttribute("form") InscriptionCandidatForm form, Model model) {
        try {
            List<CandidatInscriptionService.ExperienceSaisie> experiences = form.getExperiences().stream()
                    .filter(e -> e.getIntitule() != null && !e.getIntitule().isBlank())
                    .map(e -> new CandidatInscriptionService.ExperienceSaisie(
                            e.getIntitule(), e.getEntreprise(), e.getDateDebut(), e.getDateFin(), e.getDescription()))
                    .toList();

            candidatInscriptionService.inscrire(
                    form.getEmail(), form.getMotDePasse(), form.getNom(), form.getPrenom(), form.getTelephone(),
                    form.getDateNaissance(), form.getLieuNaissance(), form.getCin(), form.getAdresse(),
                    experiences, form.getDiplomeIds(), form.getCv());

            return "redirect:/login?inscrit";
        } catch (BusinessException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("diplomes", diplomeRepository.findAll());
            return "public/inscription";
        }
    }
}
