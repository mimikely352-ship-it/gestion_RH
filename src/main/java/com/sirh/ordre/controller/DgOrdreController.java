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
    public String liste(Model model) {
        model.addAttribute("ordres", ordreRepository.findAll());
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
}
