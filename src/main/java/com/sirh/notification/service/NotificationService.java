package com.sirh.notification.service;

import com.sirh.commun.enums.TypeNotification;
import com.sirh.commun.enums.Role;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.notification.entity.Notification;
import com.sirh.notification.repository.NotificationRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Point d'entrée unique pour émettre une notification depuis n'importe quel autre service métier. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public void notifier(Long utilisateurId, String message, TypeNotification type, String lien) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + utilisateurId));
        Notification notification = Notification.builder()
                .utilisateur(utilisateur)
                .message(message == null ? "Notification" : message.substring(0, Math.min(255, message.length())))
                .type(type)
                .lien(lien)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifierRh(String message, TypeNotification type, String lien) {
        utilisateurRepository.findByRole(Role.RH).stream()
                .filter(Utilisateur::isActif)
                .forEach(rh -> notifier(rh.getId(), message, type, lien));
    }

    public List<Notification> listerPour(Long utilisateurId) {
        return notificationRepository.findByUtilisateurIdOrderByDateCreationDesc(utilisateurId);
    }

    public long compterNonLues(Long utilisateurId) {
        return notificationRepository.countByUtilisateurIdAndLuFalse(utilisateurId);
    }

    @Transactional
    public void marquerLue(Long notificationId, String email) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable : " + notificationId));
        if (!notification.getUtilisateur().getEmail().equals(email))
            throw new org.springframework.security.access.AccessDeniedException("Cette notification ne vous appartient pas.");
        notification.setLu(true);
        notificationRepository.save(notification);
    }
}
