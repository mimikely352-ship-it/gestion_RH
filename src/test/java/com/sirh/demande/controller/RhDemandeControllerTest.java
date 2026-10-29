package com.sirh.demande.controller;

import com.sirh.commun.enums.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.demande.entity.Demande;
import com.sirh.demande.repository.DemandeRepository;
import com.sirh.demande.service.DemandeService;
import com.sirh.organisation.repository.PosteRepository;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RhDemandeControllerTest {
    private final DemandeService service = mock(DemandeService.class);
    private final DemandeRepository repository = mock(DemandeRepository.class);
    private final RhDemandeController controller = new RhDemandeController(service, repository,
            mock(UtilisateurRepository.class), mock(PosteRepository.class));

    @Test void retrouveUneDemandeApresLeDemarrageDeSonExecution() throws Exception {
        var demande = Demande.builder().id(1L).type(TypeDemande.TRANSFERT).statut(StatutDemande.EN_EXECUTION).build();
        when(repository.findByStatut(StatutDemande.EN_EXECUTION)).thenReturn(List.of(demande));
        MockMvcBuilders.standaloneSetup(controller).build().perform(get("/rh/demandes"))
                .andExpect(status().isOk()).andExpect(model().attribute("enExecution", List.of(demande)));
    }

    @Test void confirmeLaRealisationEtRevientAuDossier() throws Exception {
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/rh/demandes/1/cloturer-execution").param("dateEffet", java.time.LocalDate.now().toString()))
                .andExpect(redirectedUrl("/rh/demandes/1"))
                .andExpect(flash().attributeExists("succes"));
        verify(service).marquerExecutee(1L, java.time.LocalDate.now());
    }

    @Test void expliqueUnBlocageSansPerdreLeDossier() throws Exception {
        doThrow(new BusinessException("Service de destination manquant.")).when(service).marquerExecutee(1L, java.time.LocalDate.now());
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/rh/demandes/1/cloturer-execution").param("dateEffet", java.time.LocalDate.now().toString()))
                .andExpect(redirectedUrl("/rh/demandes/1"))
                .andExpect(flash().attribute("erreur", "Service de destination manquant."));
    }
}
