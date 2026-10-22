package com.sirh.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Isolé de {@link SecurityConfig} pour casser le cycle de construction des beans : {@link
 * JwtAuthFilter} dépend de ce repository, et {@link SecurityConfig} dépend de {@link
 * JwtAuthFilter} — le définir dans {@link SecurityConfig} créerait donc une dépendance circulaire.
 */
@Configuration
public class SecurityContextRepositoryConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new RequestAttributeSecurityContextRepository();
    }
}
