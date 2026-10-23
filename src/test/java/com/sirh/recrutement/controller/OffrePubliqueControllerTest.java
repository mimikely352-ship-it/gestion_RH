package com.sirh.recrutement.controller;

import com.sirh.commun.enums.StatutOffre;
import com.sirh.recrutement.dto.TexteAnnonce;
import com.sirh.recrutement.entity.OffreEmploi;
import com.sirh.recrutement.service.OffreEmploiService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDate;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OffrePubliqueControllerTest {
    @Test void rechercheLesOffresSansDistinguerLesAccents() throws Exception {
        var service = mock(OffreEmploiService.class);
        var reception = OffreEmploi.builder().titre("Réceptionniste").build();
        var comptable = OffreEmploi.builder().titre("Comptable").build();
        when(service.listerPubliees()).thenReturn(java.util.List.of(reception, comptable));
        MockMvcBuilders.standaloneSetup(new OffrePubliqueController(service)).build()
                .perform(get("/offres").param("q", "reception"))
                .andExpect(status().isOk()).andExpect(model().attribute("offres", java.util.List.of(reception)))
                .andExpect(model().attribute("rechercheOffres", "reception"));
    }

    @Test void prepareLesDeuxPartiesEtAccepteLesDepotsJusquaLaDateLimite() throws Exception {
        var service = mock(OffreEmploiService.class);
        when(service.obtenirPublique(1L)).thenReturn(OffreEmploi.builder().id(1L).statut(StatutOffre.PUBLIEE)
                .dateLimite(LocalDate.now()).description(TexteAnnonce.composer("Accueil", "Organisation", null)).build());
        MockMvcBuilders.standaloneSetup(new OffrePubliqueController(service)).build().perform(get("/offres/1"))
                .andExpect(status().isOk()).andExpect(view().name("public/offre-detail"))
                .andExpect(model().attribute("candidaturesOuvertes", true))
                .andExpect(model().attribute("texteAnnonce", Map.of("missions", "Accueil", "profil", "Organisation")));
    }

    @Test void fermeLeDepotSiLaDateLimiteEstPasseeOuLOffreCloturee() throws Exception {
        var service = mock(OffreEmploiService.class);
        when(service.obtenirPublique(1L)).thenReturn(OffreEmploi.builder().statut(StatutOffre.PUBLIEE).dateLimite(LocalDate.now().minusDays(1)).build());
        when(service.obtenirPublique(2L)).thenReturn(OffreEmploi.builder().statut(StatutOffre.CLOTUREE).build());
        var mvc = MockMvcBuilders.standaloneSetup(new OffrePubliqueController(service)).build();
        mvc.perform(get("/offres/1")).andExpect(model().attribute("candidaturesOuvertes", false));
        mvc.perform(get("/offres/2")).andExpect(model().attribute("candidaturesOuvertes", false));
    }
}
