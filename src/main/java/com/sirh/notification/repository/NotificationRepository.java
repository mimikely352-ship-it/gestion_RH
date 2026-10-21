package com.sirh.notification.repository;

import com.sirh.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUtilisateurIdOrderByDateCreationDesc(Long utilisateurId);
    long countByUtilisateurIdAndLuFalse(Long utilisateurId);
}
