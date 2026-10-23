package com.sirh.notification.controller;

import com.sirh.commun.enums.Role;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.notification.service.NotificationService;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/** Pages communes à tous les rôles (candidat inclus). */
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping("/notifications")
    public String liste(Authentication authentication, Model model) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
        model.addAttribute("notifications", notificationService.listerPour(utilisateur.getId()));
        return utilisateur.getRole() == Role.CANDIDAT ? "candidat/notifications" : "commun/notifications";
    }

    @PostMapping("/notifications/{id}/lu")
    public String marquerLue(@PathVariable Long id, Authentication authentication) {
        notificationService.marquerLue(id, authentication.getName());
        return "redirect:/notifications";
    }
}
