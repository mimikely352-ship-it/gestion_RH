package com.sirh.notification.service;

import com.sirh.commun.enums.Role;
import com.sirh.commun.enums.TypeNotification;
import com.sirh.notification.entity.Notification;
import com.sirh.notification.repository.NotificationRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    @Test void impossibleDeMarquerLaNotificationDunAutreCompte() {
        var repo=mock(NotificationRepository.class);
        var n=Notification.builder().utilisateur(Utilisateur.builder().email("proprietaire@test.local").build()).build();
        when(repo.findById(1L)).thenReturn(Optional.of(n));
        var sut=new NotificationService(repo,mock(UtilisateurRepository.class));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->sut.marquerLue(1L,"autre@test.local"));
        assertFalse(n.isLu()); verify(repo,never()).save(any());
    }
    @Test void descriptionLongueNeDepassePasLaColonneSql() {
        var repo=mock(NotificationRepository.class); var users=mock(UtilisateurRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(Utilisateur.builder().id(1L).build()));
        new NotificationService(repo,users).notifier(1L,"a".repeat(1000),TypeNotification.ORDRE,"/rh/ordres/1");
        verify(repo).save(argThat(n->n.getMessage().length()==255));
    }
    @Test void notifieChaqueRhActifEtIgnoreLesComptesInactifs() {
        var notifications = mock(NotificationRepository.class);
        var utilisateurs = mock(UtilisateurRepository.class);
        var premier = Utilisateur.builder().id(1L).role(Role.RH).actif(true).build();
        var second = Utilisateur.builder().id(2L).role(Role.RH).actif(true).build();
        var inactif = Utilisateur.builder().id(3L).role(Role.RH).actif(false).build();
        when(utilisateurs.findByRole(Role.RH)).thenReturn(List.of(premier, second, inactif));
        when(utilisateurs.findById(1L)).thenReturn(Optional.of(premier));
        when(utilisateurs.findById(2L)).thenReturn(Optional.of(second));
        new NotificationService(notifications, utilisateurs).notifierRh(
                "Candidature reçue", TypeNotification.CANDIDATURE, "/rh/candidatures/42");
        var capture = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(capture.capture());
        assertEquals(List.of(1L, 2L), capture.getAllValues().stream().map(n -> n.getUtilisateur().getId()).toList());
        assertTrue(capture.getAllValues().stream().allMatch(n -> !n.isLu()
                && n.getLien().equals("/rh/candidatures/42") && n.getType() == TypeNotification.CANDIDATURE));
        verify(utilisateurs, never()).findById(3L);
    }
}
