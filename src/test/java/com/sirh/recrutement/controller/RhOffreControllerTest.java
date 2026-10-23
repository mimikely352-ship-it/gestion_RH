package com.sirh.recrutement.controller;

import com.sirh.commun.exception.BusinessException;
import com.sirh.organisation.repository.PosteRepository;
import com.sirh.organisation.repository.SectionRepository;
import com.sirh.recrutement.service.CandidatureService;
import com.sirh.recrutement.service.OffreEmploiService;
import com.sirh.referentiel.repository.DiplomeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class RhOffreControllerTest {
    @Test void creeEtPublieDepuisUnSeulIntitule() throws Exception {
        var service = mock(OffreEmploiService.class);
        var entrepriseService = mock(com.sirh.recrutement.service.OffreEntrepriseService.class);
        when(entrepriseService.creer(any(), eq(true), any(), any(), any(), any(), any(), any(), anyInt(), any(), anyInt(), any(), any(), any(), any()))
                .thenReturn(com.sirh.recrutement.entity.OffreEmploi.builder().id(99L).build());
        var controller = new RhOffreController(service, mock(CandidatureService.class),
                mock(PosteRepository.class), mock(SectionRepository.class), mock(DiplomeRepository.class),
                mock(com.sirh.organisation.repository.EntrepriseRepository.class), entrepriseService);
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/rh/offres")
                .param("modePoste", "NOUVEAU").param("titre", "Commercial terrain")
                .param("missions", "Prospecter les clients").param("profil", "Sens du contact")
                .param("nbPostes", "1").param("actionOffre", "publier"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/rh/offres/99"))
                .andExpect(flash().attribute("succes", "L’offre a été publiée."));
        verify(entrepriseService).creer(isNull(), eq(true), isNull(), eq("Commercial terrain"), isNull(), isNull(),
                eq("Commercial terrain"), contains("Profil recherché\nSens du contact"), eq(1), isNull(), eq(60),
                isNull(), eq(java.util.Set.of()), isNull(), isNull());
    }

    @Test void indiqueLeChampProfilInvalideSansPerdreLesMissions() throws Exception {
        var service = mock(OffreEmploiService.class);
        var entrepriseService = mock(com.sirh.recrutement.service.OffreEntrepriseService.class);
        var controller = new RhOffreController(service, mock(CandidatureService.class),
                mock(PosteRepository.class), mock(SectionRepository.class), mock(DiplomeRepository.class),
                mock(com.sirh.organisation.repository.EntrepriseRepository.class), entrepriseService);
        var resultat = MockMvcBuilders.standaloneSetup(controller).build().perform(post("/rh/offres")
                .param("modePoste", "NOUVEAU").param("titre", "Commercial terrain")
                .param("missions", "Prospecter les clients").param("profil", " ").param("nbPostes", "1"))
                .andExpect(status().isOk()).andExpect(view().name("rh/offre-form"))
                .andExpect(model().attribute("etapeErreur", 1)).andReturn();
        var erreurs = (java.util.Map<?, ?>) resultat.getModelAndView().getModel().get("erreursChamps");
        assertTrue(erreurs.containsKey("profil"));
        var saisie = (java.util.Map<?, ?>) resultat.getModelAndView().getModel().get("saisie");
        assertEquals("Prospecter les clients", saisie.get("missions"));
        verifyNoInteractions(service);
    }

    @Test void publieLeBrouillonDepuisLaVerification() throws Exception {
        var service = mock(OffreEmploiService.class);
        var entrepriseService = mock(com.sirh.recrutement.service.OffreEntrepriseService.class);
        var controller = new RhOffreController(service, mock(CandidatureService.class),
                mock(PosteRepository.class), mock(SectionRepository.class), mock(DiplomeRepository.class),
                mock(com.sirh.organisation.repository.EntrepriseRepository.class), entrepriseService);
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/rh/offres/7/modifier")
                .param("titre", "Commercial").param("missions", "Missions").param("profil", "Profil")
                .param("nbPostes", "1").param("actionOffre", "publier"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/rh/offres/7"))
                .andExpect(flash().attribute("succes", "L’offre a été publiée."));
        verify(entrepriseService).modifier(eq(7L), isNull(), eq(true), eq("Commercial"), contains("Missions principales"), eq(1),
                isNull(), isNull(), eq(java.util.Set.of()), isNull());
    }

    @Test void publicationInvalideAfficheUnMessageSurLaFiche() throws Exception {
        var service = mock(OffreEmploiService.class);
        var entrepriseService = mock(com.sirh.recrutement.service.OffreEntrepriseService.class);
        when(service.publier(7L)).thenThrow(new BusinessException("La date limite est passée."));
        var controller = new RhOffreController(service, mock(CandidatureService.class),
                mock(PosteRepository.class), mock(SectionRepository.class), mock(DiplomeRepository.class),
                mock(com.sirh.organisation.repository.EntrepriseRepository.class), entrepriseService);
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/rh/offres/7/publier"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/rh/offres/7"))
                .andExpect(flash().attribute("erreur", "La date limite est passée."));
    }

    @Test void publicationReussieConfirmeLeResultatSurLaFiche() throws Exception {
        var service = mock(OffreEmploiService.class);
        var entrepriseService = mock(com.sirh.recrutement.service.OffreEntrepriseService.class);
        var controller = new RhOffreController(service, mock(CandidatureService.class),
                mock(PosteRepository.class), mock(SectionRepository.class), mock(DiplomeRepository.class),
                mock(com.sirh.organisation.repository.EntrepriseRepository.class), entrepriseService);
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/rh/offres/7/publier"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/rh/offres/7"))
                .andExpect(flash().attributeExists("succes"));
        verify(service).publier(7L);
    }

    @Test void preRemplitLeFormulaireDeModification() throws Exception {
        var service = mock(OffreEmploiService.class);
        var entrepriseService = mock(com.sirh.recrutement.service.OffreEntrepriseService.class);
        var poste = com.sirh.organisation.entity.Poste.builder().intitule("Commercial").build();
        var offre = com.sirh.recrutement.entity.OffreEmploi.builder().id(7L).poste(poste)
                .titre("Commercial terrain").description("Missions actuelles").nbPostes(2).experienceMinMois(12).build();
        when(service.obtenir(7L)).thenReturn(offre);
        var controller = new RhOffreController(service, mock(CandidatureService.class),
                mock(PosteRepository.class), mock(SectionRepository.class), mock(DiplomeRepository.class),
                mock(com.sirh.organisation.repository.EntrepriseRepository.class), entrepriseService);
        var resultat = MockMvcBuilders.standaloneSetup(controller).build().perform(get("/rh/offres/7/modifier"))
                .andExpect(status().isOk()).andExpect(view().name("rh/offre-form"))
                .andExpect(model().attribute("offreId", 7L)).andReturn();
        var saisie = (java.util.Map<?, ?>) resultat.getModelAndView().getModel().get("saisie");
        assertEquals("Commercial terrain", saisie.get("titre"));
        assertEquals("2", saisie.get("nbPostes"));
        assertEquals("12", saisie.get("experienceMinMois"));
    }

    @Test void conserveLaCorrectionApresUneErreurDeModification() throws Exception {
        var service = mock(OffreEmploiService.class);
        var entrepriseService = mock(com.sirh.recrutement.service.OffreEntrepriseService.class);
        var offre = com.sirh.recrutement.entity.OffreEmploi.builder().id(7L).titre("Ancien titre").build();
        when(service.obtenir(7L)).thenReturn(offre);
        doThrow(new BusinessException("Expérience négative")).when(entrepriseService)
                .modifier(eq(7L), any(), anyBoolean(), any(), any(), anyInt(), any(), any(), any(), any());
        var controller = new RhOffreController(service, mock(CandidatureService.class),
                mock(PosteRepository.class), mock(SectionRepository.class), mock(DiplomeRepository.class),
                mock(com.sirh.organisation.repository.EntrepriseRepository.class), entrepriseService);
        var resultat = MockMvcBuilders.standaloneSetup(controller).build().perform(post("/rh/offres/7/modifier")
                .param("titre", "Titre corrigé").param("description", "Missions corrigées")
                .param("nbPostes", "2").param("experienceMinMois", "-1").param("diplomeIds", "3"))
                .andExpect(status().isOk()).andExpect(view().name("rh/offre-form"))
                .andExpect(model().attribute("offreId", 7L))
                .andExpect(model().attribute("diplomesSelectionnes", java.util.Set.of(3L))).andReturn();
        var saisie = (java.util.Map<?, ?>) resultat.getModelAndView().getModel().get("saisie");
        assertEquals("Titre corrigé", saisie.get("titre"));
        assertEquals("Missions corrigées", saisie.get("description"));
    }

    @Test void erreurMetierConserveLaSaisieEtLesDiplomes() throws Exception {
        var service = mock(OffreEmploiService.class);
        var entrepriseService = mock(com.sirh.recrutement.service.OffreEntrepriseService.class);
        when(entrepriseService.creer(any(), anyBoolean(), any(), any(), any(), any(), any(), any(), anyInt(), any(), anyInt(), any(), any(), any(), any()))
                .thenThrow(new BusinessException("Expérience négative"));
        var controller = new RhOffreController(service, mock(CandidatureService.class),
                mock(PosteRepository.class), mock(SectionRepository.class), mock(DiplomeRepository.class),
                mock(com.sirh.organisation.repository.EntrepriseRepository.class), entrepriseService);
        var resultat = MockMvcBuilders.standaloneSetup(controller).build().perform(post("/rh/offres")
                .param("modePoste", "NOUVEAU").param("nouvelIntitule", "Designer")
                .param("description", "Nos missions").param("nbPostes", "2")
                .param("experienceMinMois", "-1").param("diplomeIds", "1", "2"))
                .andExpect(status().isOk()).andExpect(view().name("rh/offre-form"))
                .andExpect(model().attribute("erreur", "Expérience négative"))
                .andExpect(model().attribute("diplomesSelectionnes", java.util.Set.of(1L, 2L)))
                .andReturn();
        var saisie = (java.util.Map<?, ?>) resultat.getModelAndView().getModel().get("saisie");
        assertEquals("Designer", saisie.get("nouvelIntitule"));
        assertEquals("Nos missions", saisie.get("description"));
    }

    @Test void exigeUnIntituleEnModePersonnalise() throws Exception {
        var service = mock(OffreEmploiService.class);
        var entrepriseService = mock(com.sirh.recrutement.service.OffreEntrepriseService.class);
        var controller = new RhOffreController(service, mock(CandidatureService.class),
                mock(PosteRepository.class), mock(SectionRepository.class), mock(DiplomeRepository.class),
                mock(com.sirh.organisation.repository.EntrepriseRepository.class), entrepriseService);
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/rh/offres")
                .param("modePoste", "NOUVEAU").param("description", "Missions").param("nbPostes", "1"))
                .andExpect(status().isOk()).andExpect(view().name("rh/offre-form"))
                .andExpect(model().attributeExists("erreur"));
        verifyNoInteractions(service);
    }
}
