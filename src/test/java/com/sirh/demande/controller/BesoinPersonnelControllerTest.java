package com.sirh.demande.controller;

import com.sirh.commun.enums.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.demande.entity.Demande;
import com.sirh.demande.service.DemandeService;
import com.sirh.organisation.repository.PosteRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BesoinPersonnelControllerTest {
    private final DemandeService demandes = mock(DemandeService.class);
    private final PosteRepository postes = mock(PosteRepository.class);
    private final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    private final BesoinPersonnelController controller = new BesoinPersonnelController(demandes, postes, utilisateurs);
    private final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("chef@test.local", "");

    @Test void listeLimiteeAuxBesoinsDuCompteConnecte() throws Exception {
        when(utilisateurs.findByEmail("chef@test.local")).thenReturn(Optional.of(Utilisateur.builder().id(2L).build()));
        var besoin = Demande.builder().type(TypeDemande.RECRUTEMENT).build();
        when(demandes.listerPourAuteur(2L)).thenReturn(List.of(besoin, Demande.builder().type(TypeDemande.FORMATION).build()));
        MockMvcBuilders.standaloneSetup(controller).build().perform(get("/besoins").principal(auth))
                .andExpect(status().isOk()).andExpect(model().attribute("demandes", List.of(besoin)));
    }
    @Test void soumissionUtiliseAuteurConnecteEtSalaireSansMultiplication() throws Exception {
        when(utilisateurs.findByEmail("chef@test.local")).thenReturn(Optional.of(Utilisateur.builder().id(2L).build()));
        var date = LocalDate.now().plusDays(10);
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/besoins").principal(auth)
                .param("posteCibleId", "9").param("description", "Renfort").param("effectifDemande", "3")
                .param("salaireMensuelParPoste", "1250000.50").param("budgetGlobalRecrutement", "45000018")
                .param("dateSouhaitee", date.toString())
                .param("motifBesoin", "CREATION").param("auteurId", "999"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/besoins"));
        verify(demandes).creer(2L, TypeDemande.RECRUTEMENT, 9L, "Renfort", 3, new BigDecimal("1250000.50"), new BigDecimal("45000018"), date, MotifBesoin.CREATION);
    }
    @Test void erreurConserveLesValeursDuFormulaire() throws Exception {
        when(utilisateurs.findByEmail("chef@test.local")).thenReturn(Optional.of(Utilisateur.builder().id(2L).build()));
        when(demandes.creer(eq(2L), eq(TypeDemande.RECRUTEMENT), eq(9L), eq("Besoin"), eq(0), isNull(), isNull(), isNull(), isNull()))
                .thenThrow(new BusinessException("Effectif invalide."));
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/besoins").principal(auth)
                .param("posteCibleId", "9").param("description", "Besoin").param("effectifDemande", "0"))
                .andExpect(status().isOk()).andExpect(view().name("employe/demande-form"))
                .andExpect(model().attribute("effectifDemande", 0)).andExpect(model().attribute("actionDemande", "/besoins"));
    }
}
