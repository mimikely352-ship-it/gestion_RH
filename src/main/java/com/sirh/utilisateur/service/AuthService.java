package com.sirh.utilisateur.service;

import com.sirh.config.JwtService;
import com.sirh.commun.enums.Role;
import com.sirh.utilisateur.dto.LoginResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final com.sirh.utilisateur.repository.UtilisateurRepository utilisateurs;

    @Value("${app.jwt.cookie-name}")
    private String cookieName;

    @Value("${app.jwt.expiration-minutes}")
    private long expirationMinutes;

    @Value("${app.jwt.cookie-secure}")
    private boolean cookieSecure;

    /** Authentifie puis génère le JWT. Ne pose pas le cookie : voir {@link #poserCookie}. */
    public LoginResponse authentifier(String email, String motDePasse) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, motDePasse));
        UtilisateurPrincipal principal = (UtilisateurPrincipal) authentication.getPrincipal();
        if (!principal.getUtilisateur().isActif()) {
            throw new BadCredentialsException("Compte désactivé");
        }
        Role role = principal.getUtilisateur().getRole();
        String token = jwtService.generer(principal.getId(), principal.getUsername(), role.name());
        return new LoginResponse(token, principal.getId(), principal.getUsername(), role.name());
    }

    public void poserCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(cookieName, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge((int) (expirationMinutes * 60));
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    /** Appelé uniquement après la transaction réussie de création d'un nouveau candidat. */
    public void connecterCandidatInscrit(HttpServletResponse response, com.sirh.utilisateur.entity.Utilisateur utilisateur) {
        if (utilisateur.getId() == null || utilisateur.getRole() != Role.CANDIDAT || !utilisateur.isActif())
            throw new IllegalArgumentException("Compte candidat invalide.");
        poserCookie(response,jwtService.generer(utilisateur.getId(),utilisateur.getEmail(),utilisateur.getRole().name()));
    }

    public void supprimerCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSecure(cookieSecure);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @org.springframework.transaction.annotation.Transactional
    public void revoquer(String email) {
        utilisateurs.findByEmail(email).ifPresent(u -> {
            u.setTokensValidesDepuis(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
            utilisateurs.save(u);
        });
    }

    /** Chemin d'accueil par défaut après connexion, selon le rôle. */
    public String accueilPour(Role role) {
        return switch (role) {
            case CANDIDAT -> "/candidat/mes-candidatures";
            case EMPLOYE -> "/employe/tableau-de-bord";
            case CHEF_SERVICE -> "/chef-service/tableau-de-bord";
            case DIRECTION -> "/direction/tableau-de-bord";
            case RH -> "/rh/tableau-de-bord";
            case DG -> "/dg/tableau-de-bord";
        };
    }
}
