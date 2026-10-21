package com.sirh.notification.service;

import com.sirh.commun.enums.TypeNotification;
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
                .message(message)
                .type(type)
                .lien(lien)
                .build();
        notificationRepository.save(notification);
    }

    public List<Notification> listerPour(Long utilisateurId) {
        return notificationRepository.findByUtilisateurIdOrderByDateCreationDesc(utilisateurId);
    }

    public long compterNonLues(Long utilisateurId) {
        return notificationRepository.countByUtilisateurIdAndLuFalse(utilisateurId);
    }

    @Transactional
    public void marquerLue(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable : " + notificationId));
        notification.setLu(true);
        notificationRepository.save(notification);
    }
}
