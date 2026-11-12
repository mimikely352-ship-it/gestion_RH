package com.sirh.presence.controller;

import com.sirh.commun.exception.BusinessException;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.presence.entity.DemandeConge;
import com.sirh.presence.entity.StatutConge;
import com.sirh.presence.service.AccesPresenceService;
import com.sirh.presence.service.CongeService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.*;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CongeControllerTest {
    private final CongeService conges = mock(CongeService.class);
    private final AccesPresenceService acces = mock(AccesPresenceService.class);
    private final CongeController controller = new CongeController(conges,acces,mock(DossierEmployeRepository.class),Clock.systemUTC());
    private final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("emp@test","");
    private final LocalDate debut = LocalDate.of(2027,1,4), fin = LocalDate.of(2027,1,8);

    @Test void erreurMetierConserveLaSaisieEtDemandeDeReselectionnerLePdf() throws Exception {
        var pdf = new MockMultipartFile("justificatif","piece.pdf","application/pdf","%PDF-1.4".getBytes());
        when(conges.soumettre(1L,debut,fin,"Repos",pdf,auth)).thenThrow(new BusinessException("Solde insuffisant."));
        MockMvcBuilders.standaloneSetup(controller).build().perform(multipart("/conges").file(pdf).principal(auth)
                .param("typeId","1").param("debut",debut.toString()).param("fin",fin.toString()).param("motif","Repos"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/conges?annee=2027"))
                .andExpect(flash().attribute("erreur","Solde insuffisant."))
                .andExpect(flash().attribute("congeSaisie",Map.of("typeId",1L,"debut",debut,"fin",fin,"motif","Repos")))
                .andExpect(flash().attribute("pieceAReselectionner",true));
    }

    @Test void succesNeReproposePasUneDemandeDejaEnvoyee() throws Exception {
        when(conges.soumettre(1L,debut,fin,"Repos",null,auth))
                .thenReturn(DemandeConge.builder().jours(5).statut(StatutConge.ATTENTE_RH).build());
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/conges").principal(auth)
                .param("typeId","1").param("debut",debut.toString()).param("fin",fin.toString()).param("motif","Repos"))
                .andExpect(redirectedUrl("/conges"))
                .andExpect(flash().attribute("succes","Demande envoyée : 5 jour(s) réservé(s), en attente de la décision des RH."))
                .andExpect(flash().attributeCount(1));
    }

    @Test void anneeInvalideNeRedirigePasVersUnSoldeInconsultable() throws Exception {
        var date = LocalDate.of(2201,1,1);
        when(conges.soumettre(1L,date,date,"Repos",null,auth)).thenThrow(new BusinessException("Année invalide."));
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/conges").principal(auth)
                .param("typeId","1").param("debut",date.toString()).param("fin",date.toString()).param("motif","Repos"))
                .andExpect(redirectedUrl("/conges")).andExpect(flash().attribute("erreur","Année invalide."));
    }
}
