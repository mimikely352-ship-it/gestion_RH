package com.sirh.ordre.service;
import com.sirh.commun.enums.StatutOrdre;
import com.sirh.commun.exception.BusinessException;
import com.sirh.ordre.repository.OrdreRepository;
import com.sirh.ordre.entity.Ordre;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import com.sirh.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class OrdreServiceTest {
    @Test void apiNePeutContournerDestinataireNiTransitions() {
        var repo=mock(OrdreRepository.class);
        var sut=new OrdreService(repo,mock(UtilisateurRepository.class),mock(NotificationService.class));
        var ordre=Ordre.builder().rh(Utilisateur.builder().email("destinataire@test.local").build()).statut(StatutOrdre.EMIS).build();
        when(repo.findById(1L)).thenReturn(Optional.of(ordre));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->sut.marquerExecute(1L,"autre@test.local"));
        assertThrows(BusinessException.class,()->sut.marquerExecute(1L,"destinataire@test.local"));
        ordre.setStatut(StatutOrdre.EXECUTE);
        assertThrows(BusinessException.class,()->sut.marquerEnExecution(1L,"destinataire@test.local"));
        verify(repo,never()).save(any());
    }
}
