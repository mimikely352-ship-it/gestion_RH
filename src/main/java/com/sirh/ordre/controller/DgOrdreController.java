package com.sirh.ordre.controller;

import com.sirh.commun.enums.Role;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.ordre.repository.OrdreRepository;
import com.sirh.ordre.service.OrdreService;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/dg/ordres")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DG')")
public class DgOrdreController {

    private final OrdreService ordreService;
    private final OrdreRepository ordreRepository;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    public String liste(Model model, Authentication authentication) {
        model.addAttribute("ordres", ordreRepository.findAll().stream()
                .filter(o -> o.getDg().getEmail().equals(authentication.getName())).toList());
        model.addAttribute("rhs", utilisateurRepository.findByRole(Role.RH));
        return "dg/ordres-liste";
    }

    @PostMapping
    public String emettre(Authentication authentication, @RequestParam Long rhId,
                           @RequestParam String description, @RequestParam(defaultValue = "7") int delaiJours) {
        Long dgId = utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable.")).getId();
        ordreService.emettre(dgId, rhId, description, delaiJours);
        return "redirect:/dg/ordres";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        var ordre = ordreService.obtenir(id);
        if (!ordre.getDg().getEmail().equals(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("Cet ordre appartient à un autre directeur général.");
        }
        model.addAttribute("ordre", ordre);
        model.addAttribute("espaceRh", false);
        return "commun/ordre-detail";
    }
}
