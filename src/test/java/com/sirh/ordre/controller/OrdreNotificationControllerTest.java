package com.sirh.ordre.controller;

import com.sirh.ordre.entity.Ordre;
import com.sirh.ordre.service.OrdreService;
import com.sirh.ordre.repository.OrdreRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrdreNotificationControllerTest {
    final OrdreService service = mock(OrdreService.class);
    final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    final Ordre ordre = Ordre.builder().id(1L)
            .rh(Utilisateur.builder().email("rh@test.local").build())
            .dg(Utilisateur.builder().email("dg@test.local").build()).build();

    @Test void ancienLienRhOuvreLeDetail() throws Exception {
        when(service.obtenir(1L)).thenReturn(ordre);
        MockMvcBuilders.standaloneSetup(new RhOrdreController(service, utilisateurs)).build()
                .perform(get("/rh/ordres/1").principal(new UsernamePasswordAuthenticationToken("rh@test.local", null)))
                .andExpect(status().isOk()).andExpect(view().name("commun/ordre-detail"))
                .andExpect(model().attribute("ordre", ordre)).andExpect(model().attribute("espaceRh", true));
    }

    @Test void lienDgOuvreLeDetail() throws Exception {
        when(service.obtenir(1L)).thenReturn(ordre);
        MockMvcBuilders.standaloneSetup(new DgOrdreController(service, mock(OrdreRepository.class), utilisateurs)).build()
                .perform(get("/dg/ordres/1").principal(new UsernamePasswordAuthenticationToken("dg@test.local", null)))
                .andExpect(status().isOk()).andExpect(view().name("commun/ordre-detail"))
                .andExpect(model().attribute("espaceRh", false));
    }

    @Test void autreRhNePeutNiLireNiExecuter() {
        when(service.obtenir(1L)).thenReturn(ordre);
        var controller = new RhOrdreController(service, utilisateurs);
        var autre = new UsernamePasswordAuthenticationToken("autre@test.local", null);
        assertThrows(AccessDeniedException.class, () -> controller.detail(1L, autre, new ExtendedModelMap()));
        assertThrows(AccessDeniedException.class, () -> controller.demarrer(1L, autre));
        assertThrows(AccessDeniedException.class, () -> controller.terminer(1L, autre));
        verify(service, never()).marquerEnExecution(anyLong(), anyString());
        verify(service, never()).marquerExecute(anyLong(), anyString());
    }

    @Test void autreDgNePeutPasLire() {
        when(service.obtenir(1L)).thenReturn(ordre);
        var controller = new DgOrdreController(service, mock(OrdreRepository.class), utilisateurs);
        assertThrows(AccessDeniedException.class, () -> controller.detail(1L,
                new UsernamePasswordAuthenticationToken("autre@test.local", null), new ExtendedModelMap()));
    }
}
