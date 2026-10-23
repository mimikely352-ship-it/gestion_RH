package com.sirh.config;

import com.sirh.utilisateur.service.UtilisateurDetailsService;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CompteSecurityTest {
    AnnotationConfigWebApplicationContext contexte;
    MockMvc mvc;

    @Configuration
    @EnableWebMvc
    static class ConfigurationTest {
        @Bean UtilisateurDetailsService details() { return mock(UtilisateurDetailsService.class); }
        @Bean SecurityContextRepository repository() { return new RequestAttributeSecurityContextRepository(); }
        @Bean JwtService jwt() { return new JwtService("cle-test-assez-longue-pour-hmac-256-et-tests", 60); }
        @Bean com.sirh.utilisateur.repository.UtilisateurRepository utilisateurs() {
            var repo = mock(com.sirh.utilisateur.repository.UtilisateurRepository.class);
            org.mockito.Mockito.when(repo.findByEmail(org.mockito.ArgumentMatchers.anyString())).thenAnswer(i -> {
                String email = i.getArgument(0);
                return java.util.Optional.of(com.sirh.utilisateur.entity.Utilisateur.builder().id(1L).email(email).actif(true)
                    .role(com.sirh.commun.enums.Role.valueOf(email.split("@")[0])).build());
            });
            return repo;
        }
        @Bean JwtAuthFilter filtre(JwtService jwt, SecurityContextRepository repository,
                com.sirh.utilisateur.repository.UtilisateurRepository utilisateurs) {
            return new JwtAuthFilter(jwt, "test-token", repository, utilisateurs);
        }
        @Bean Pages pages() { return new Pages(); }
    }
    @RestController
    static class Pages {
        @GetMapping("/employe/demandes") String demandes() { return "demandes employe"; }
        @PostMapping("/employe/demandes") String creer() { return "cree"; }
        @PostMapping("/api/verification") String api() { return "ok"; }
    }

    @BeforeEach void preparer() {
        contexte = new AnnotationConfigWebApplicationContext();
        contexte.setServletContext(new MockServletContext());
        contexte.register(ConfigurationTest.class, SecurityConfig.class);
        contexte.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(contexte)
                .addFilters(contexte.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
    }
    @AfterEach void fermer() { contexte.close(); }
    String token(String role) { return contexte.getBean(JwtService.class).generer(1L, role + "@test.local", role); }

    @Test void rhNeVoitPasLaPageEmploye() throws Exception {
        mvc.perform(get("/employe/demandes").header("Authorization", "Bearer " + token("RH")))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/compte/accueil"));
    }
    @Test void employeVoitSesDemandes() throws Exception {
        mvc.perform(get("/employe/demandes").header("Authorization", "Bearer " + token("EMPLOYE")))
                .andExpect(status().isOk()).andExpect(content().string("demandes employe"));
    }
    @Test void changementDeCompteEntreDeuxRequetesNeConservePasLEmploye() throws Exception {
        mvc.perform(get("/employe/demandes").header("Authorization", "Bearer " + token("EMPLOYE")))
                .andExpect(status().isOk());
        mvc.perform(get("/employe/demandes").header("Authorization", "Bearer " + token("RH")))
                .andExpect(status().is3xxRedirection());
    }

    @Test void apiRefuseUnCookieSeulEtAccepteBearerSansCsrf() throws Exception {
        var jeton = token("RH");
        mvc.perform(post("/api/verification").cookie(new jakarta.servlet.http.Cookie("test-token",jeton)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/verification").header("Authorization","Bearer "+jeton)).andExpect(status().isOk());
    }

    @Test void refuseCompteDesactiveEtJetonRevoque() throws Exception {
        var utilisateurs = contexte.getBean(com.sirh.utilisateur.repository.UtilisateurRepository.class);
        var u = com.sirh.utilisateur.entity.Utilisateur.builder().id(1L).email("EMPLOYE@test.local")
                .role(com.sirh.commun.enums.Role.EMPLOYE).actif(false).build();
        org.mockito.Mockito.when(utilisateurs.findByEmail(u.getEmail())).thenReturn(java.util.Optional.of(u));
        var jeton=token("EMPLOYE");
        mvc.perform(get("/employe/demandes").header("Authorization","Bearer "+jeton)).andExpect(status().isForbidden());
        u.setActif(true); u.setTokensValidesDepuis(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusSeconds(1));
        mvc.perform(get("/employe/demandes").header("Authorization","Bearer "+jeton)).andExpect(status().isForbidden());
    }
}
