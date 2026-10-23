package com.sirh.utilisateur.service;

import com.sirh.commun.enums.Role;
import com.sirh.commun.exception.BusinessException;
import com.sirh.organisation.entity.Service;
import com.sirh.organisation.repository.ServiceRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RattachementServiceTest {
    private final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    private final ServiceRepository services = mock(ServiceRepository.class);
    private final RattachementService sut = new RattachementService(utilisateurs, services);

    @Test
    void completeLeServiceManquant() {
        var employe = Utilisateur.builder().role(Role.EMPLOYE).actif(true).build();
        var chef = Utilisateur.builder().role(Role.CHEF_SERVICE).actif(true).build();
        var service = Service.builder().chefService(chef).build();
        when(utilisateurs.findById(1L)).thenReturn(Optional.of(employe));
        when(services.findById(2L)).thenReturn(Optional.of(service));
        sut.completer(1L, 2L);
        assertSame(service, employe.getService());
        verify(utilisateurs).save(employe);
    }

    @Test
    void neContournePasLeCircuitDeTransfert() {
        var employe = Utilisateur.builder().role(Role.EMPLOYE).actif(true)
                .service(Service.builder().id(3L).build()).build();
        when(utilisateurs.findById(1L)).thenReturn(Optional.of(employe));
        assertThrows(BusinessException.class, () -> sut.completer(1L, 2L));
        assertEquals(3L, employe.getService().getId());
        verify(utilisateurs, never()).save(any());
    }

    @Test
    void refuseServiceSansResponsable() {
        var employe = Utilisateur.builder().role(Role.EMPLOYE).actif(true).build();
        when(utilisateurs.findById(1L)).thenReturn(Optional.of(employe));
        when(services.findById(2L)).thenReturn(Optional.of(Service.builder().build()));
        assertThrows(BusinessException.class, () -> sut.completer(1L, 2L));
        assertNull(employe.getService());
        verify(utilisateurs, never()).save(any());
    }
}
