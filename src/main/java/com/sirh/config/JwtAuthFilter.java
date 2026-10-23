package com.sirh.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Lit le JWT depuis le cookie httpOnly (pages JSP) ou l'en-tête Authorization (client API externe)
 * et peuple le SecurityContext. Un seul filtre pour les deux usages, conformément au choix
 * "JWT partout" validé pour ce projet.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final String cookieName;
    private final SecurityContextRepository securityContextRepository;
    private final com.sirh.utilisateur.repository.UtilisateurRepository utilisateurs;

    public JwtAuthFilter(JwtService jwtService, @Value("${app.jwt.cookie-name}") String cookieName,
                          SecurityContextRepository securityContextRepository,
                          com.sirh.utilisateur.repository.UtilisateurRepository utilisateurs) {
        this.jwtService = jwtService;
        this.cookieName = cookieName;
        this.securityContextRepository = securityContextRepository;
        this.utilisateurs = utilisateurs;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        extraireToken(request).flatMap(jwtService::parser)
                .ifPresent(claims -> authentifier(claims, request, response));
        filterChain.doFilter(request, response);
    }

    private Optional<String> extraireToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        if (request.getRequestURI().substring(request.getContextPath().length()).startsWith("/api/")) return Optional.empty();
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return Optional.of(cookie.getValue());
                }
            }
        }
        return Optional.empty();
    }

    private void authentifier(Claims claims, HttpServletRequest request, HttpServletResponse response) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        var utilisateur = utilisateurs.findByEmail(email).orElse(null);
        if (utilisateur == null || !utilisateur.isActif() || !utilisateur.getRole().name().equals(role)) return;
        Number utilisateurId = claims.get("userId", Number.class);
        if (utilisateurId == null || !utilisateur.getId().equals(utilisateurId.longValue())) return;
        Number instant = claims.get("authTime", Number.class);
        long emission = instant == null ? claims.getIssuedAt().getTime() : instant.longValue();
        if (utilisateur.getTokensValidesDepuis() != null && emission <= utilisateur.getTokensValidesDepuis()
                .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()) return;
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // Sans cette sauvegarde explicite, SessionManagementFilter ne voit jamais de contexte déjà
        // établi pour la requête (voir le commentaire sur securityContextRepository() dans
        // SecurityConfig) et régénère le cookie CSRF à chaque requête authentifiée.
        securityContextRepository.saveContext(context, request, response);
    }
}
