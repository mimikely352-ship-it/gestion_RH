package com.sirh.ordre.controller;

import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.ordre.service.OrdreService;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rh/ordres")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RH')")
public class RhOrdreController {

    private final OrdreService ordreService;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    public String liste(Authentication authentication, Model model) {
        Long rhId = utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable.")).getId();
        model.addAttribute("ordres", ordreService.listerPourRh(rhId));
        return "rh/ordres-liste";
    }

    @PostMapping("/{id}/demarrer")
    public String demarrer(@PathVariable Long id, Authentication authentication) {
        verifierDestinataire(id, authentication);
        ordreService.marquerEnExecution(id, authentication.getName());
        return "redirect:/rh/ordres";
    }

    @PostMapping("/{id}/terminer")
    public String terminer(@PathVariable Long id, Authentication authentication) {
        verifierDestinataire(id, authentication);
        ordreService.marquerExecute(id, authentication.getName());
        return "redirect:/rh/ordres";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        var ordre = verifierDestinataire(id, authentication);
        model.addAttribute("ordre", ordre);
        model.addAttribute("espaceRh", true);
        return "commun/ordre-detail";
    }

    private com.sirh.ordre.entity.Ordre verifierDestinataire(Long id, Authentication authentication) {
        var ordre = ordreService.obtenir(id);
        if (!ordre.getRh().getEmail().equals(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("Cet ordre est destiné à un autre compte RH.");
        }
        return ordre;
    }
}
