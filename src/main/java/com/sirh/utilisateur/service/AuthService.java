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
        response.addCookie(cookie);
    }

    public void supprimerCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
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
