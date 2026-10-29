package com.sirh.organisation.controller;

import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.organisation.entity.*;
import com.sirh.organisation.repository.*;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Gestion par le RH de la hiérarchie organisationnelle (direction/département/service/section/poste)
 * et des responsables qui lui sont rattachés (facultatifs). */
@Controller
@RequestMapping("/rh/organisation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RH')")
public class OrganisationAdminController {

    private final DirectionRepository directions;
    private final DepartementRepository departements;
    private final ServiceRepository services;
    private final SectionRepository sections;
    private final PosteRepository postes;
    private final UtilisateurRepository utilisateurs;

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("directions", directions.findAll());
        model.addAttribute("departements", departements.findAll());
        model.addAttribute("servicesListe", services.findAll());
        model.addAttribute("sections", sections.findAll());
        model.addAttribute("postes", postes.findAll());
        model.addAttribute("utilisateurs", utilisateurs.findByActifTrueOrderByEmailAsc());
        return "rh/organisation";
    }

    @PostMapping("/directions")
    @Transactional
    public String creerDirection(@RequestParam String nom, @RequestParam(required = false) Long directeurId,
                                  RedirectAttributes flash) {
        try {
            directions.save(Direction.builder().nom(nomValide(nom)).directeur(utilisateurOptionnel(directeurId)).build());
            flash.addFlashAttribute("succes", "Direction créée.");
        } catch (BusinessException | ResourceNotFoundException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/rh/organisation";
    }

    @PostMapping("/directions/{id}/directeur")
    @Transactional
    public String changerDirecteur(@PathVariable Long id, @RequestParam(required = false) Long directeurId,
                                    RedirectAttributes flash) {
        var direction = directions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Direction introuvable."));
        direction.setDirecteur(utilisateurOptionnel(directeurId));
        directions.save(direction);
        flash.addFlashAttribute("succes", "Directeur mis à jour.");
        return "redirect:/rh/organisation";
    }

    @PostMapping("/departements")
    @Transactional
    public String creerDepartement(@RequestParam String nom, @RequestParam Long directionId,
                                    @RequestParam(required = false) Long chefDepartementId, RedirectAttributes flash) {
        try {
            var direction = directions.findById(directionId).orElseThrow(() -> new ResourceNotFoundException("Direction introuvable."));
            departements.save(Departement.builder().nom(nomValide(nom)).direction(direction)
                    .chefDepartement(utilisateurOptionnel(chefDepartementId)).build());
            flash.addFlashAttribute("succes", "Département créé.");
        } catch (BusinessException | ResourceNotFoundException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/rh/organisation";
    }

    @PostMapping("/departements/{id}/chef")
    @Transactional
    public String changerChefDepartement(@PathVariable Long id, @RequestParam(required = false) Long chefDepartementId,
                                          RedirectAttributes flash) {
        var departement = departements.findById(id).orElseThrow(() -> new ResourceNotFoundException("Département introuvable."));
        departement.setChefDepartement(utilisateurOptionnel(chefDepartementId));
        departements.save(departement);
        flash.addFlashAttribute("succes", "Chef de département mis à jour.");
        return "redirect:/rh/organisation";
    }

    @PostMapping("/services")
    @Transactional
    public String creerService(@RequestParam String nom, @RequestParam Long departementId,
                                @RequestParam(required = false) Long chefServiceId, RedirectAttributes flash) {
        try {
            var departement = departements.findById(departementId).orElseThrow(() -> new ResourceNotFoundException("Département introuvable."));
            services.save(com.sirh.organisation.entity.Service.builder().nom(nomValide(nom)).departement(departement)
                    .chefService(utilisateurOptionnel(chefServiceId)).build());
            flash.addFlashAttribute("succes", "Service créé.");
        } catch (BusinessException | ResourceNotFoundException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/rh/organisation";
    }

    @PostMapping("/services/{id}/chef")
    @Transactional
    public String changerChefService(@PathVariable Long id, @RequestParam(required = false) Long chefServiceId,
                                      RedirectAttributes flash) {
        var service = services.findById(id).orElseThrow(() -> new ResourceNotFoundException("Service introuvable."));
        service.setChefService(utilisateurOptionnel(chefServiceId));
        services.save(service);
        flash.addFlashAttribute("succes", "Chef de service mis à jour.");
        return "redirect:/rh/organisation";
    }

    @PostMapping("/sections")
    @Transactional
    public String creerSection(@RequestParam String nom, @RequestParam Long serviceId, RedirectAttributes flash) {
        try {
            var service = services.findById(serviceId).orElseThrow(() -> new ResourceNotFoundException("Service introuvable."));
            sections.save(Section.builder().nom(nomValide(nom)).service(service).build());
            flash.addFlashAttribute("succes", "Section créée.");
        } catch (BusinessException | ResourceNotFoundException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/rh/organisation";
    }

    @PostMapping("/postes")
    @Transactional
    public String creerPoste(@RequestParam String intitule, @RequestParam(required = false) Long sectionId,
                              @RequestParam(required = false) String categorie,
                              @RequestParam(defaultValue = "false") boolean mobiliteOuverte, RedirectAttributes flash) {
        try {
            if (intitule == null || intitule.isBlank()) throw new BusinessException("L’intitulé du poste est requis.");
            Section section = sectionId == null ? null : sections.findById(sectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Section introuvable."));
            postes.save(Poste.builder().intitule(intitule.trim()).section(section)
                    .categorie(categorie == null || categorie.isBlank() ? null : categorie.trim())
                    .mobiliteOuverte(mobiliteOuverte).build());
            flash.addFlashAttribute("succes", "Poste créé.");
        } catch (BusinessException | ResourceNotFoundException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/rh/organisation";
    }

    private String nomValide(String nom) {
        if (nom == null || nom.isBlank()) throw new BusinessException("Le nom est requis.");
        if (nom.length() > 100) throw new BusinessException("Le nom est limité à 100 caractères.");
        return nom.trim();
    }

    private com.sirh.utilisateur.entity.Utilisateur utilisateurOptionnel(Long id) {
        return id == null ? null : utilisateurs.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
    }
}
