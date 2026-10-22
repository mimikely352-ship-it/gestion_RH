package com.sirh.config;

import com.sirh.utilisateur.service.UtilisateurDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Authentification JWT (cookie httpOnly pour les JSP, en-tête Authorization pour un client API
 * externe). Pas de session HTTP (STATELESS) : le filtre {@link JwtAuthFilter} reconstruit le
 * contexte de sécurité à chaque requête. Le CSRF reste actif pour les formulaires JSP (seul
 * rempart contre le CSRF puisque le cookie JWT est envoyé automatiquement par le navigateur) et
 * est désactivé pour /api/** (clients non-navigateur authentifiés par en-tête, sans cookie).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UtilisateurDetailsService utilisateurDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(utilisateurDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     SecurityContextRepository securityContextRepository) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/**"))
            .securityContext(context -> context.securityContextRepository(securityContextRepository))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/accueil", "/login", "/deconnexion",
                    "/candidats/inscription", "/candidats/inscription/**",
                    "/offres", "/offres/**",
                    "/assets/**",
                    "/api/auth/**", "/api/offres", "/api/offres/**", "/api/referentiels/**", "/api/organisation/**",
                    "/error",
                    // Spring Security réévalue aussi le *forward* interne vers la JSP résolue
                    // (InternalResourceViewResolver) : pas accessible directement par un client
                    // (le conteneur servlet bloque /WEB-INF/** par spécification), donc sans risque.
                    "/WEB-INF/**"
                ).permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
