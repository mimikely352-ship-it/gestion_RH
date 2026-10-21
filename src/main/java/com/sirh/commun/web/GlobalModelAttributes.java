package com.sirh.commun.web;

import com.sirh.notification.service.NotificationService;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

/**
 * Rend disponibles dans toutes les JSP (pages rendues par un @Controller, jamais les @RestController)
 * l'utilisateur connecté et son nombre de notifications non lues, pour que layout.tag puisse
 * afficher la cloche de notification et l'identité sans que chaque contrôleur ne le refasse.
 */
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;

    @ModelAttribute("utilisateurConnecte")
    public Utilisateur utilisateurConnecte(Authentication authentication) {
        return utilisateurOptionnel(authentication).orElse(null);
    }

    @ModelAttribute("notificationsNonLues")
    public long notificationsNonLues(Authentication authentication) {
        return utilisateurOptionnel(authentication)
                .map(u -> notificationService.compterNonLues(u.getId()))
                .orElse(0L);
    }

    private Optional<Utilisateur> utilisateurOptionnel(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return utilisateurRepository.findByEmail(authentication.getName());
    }
}
