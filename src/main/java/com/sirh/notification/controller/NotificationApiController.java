package com.sirh.notification.controller;

import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.notification.dto.NotificationDto;
import com.sirh.notification.service.NotificationService;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    public List<NotificationDto> lister(Authentication authentication) {
        Long id = utilisateurId(authentication);
        return notificationService.listerPour(id).stream().map(NotificationDto::new).toList();
    }

    @PutMapping("/{id}/lu")
    public void marquerLue(@PathVariable Long id) {
        notificationService.marquerLue(id);
    }

    private Long utilisateurId(Authentication authentication) {
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable.")).getId();
    }
}
