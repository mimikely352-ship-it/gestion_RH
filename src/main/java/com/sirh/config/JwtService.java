package com.sirh.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/** Génère et valide les JWT (signature HS256) utilisés à la fois par les JSP (cookie) et l'API REST. */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generer(Long utilisateurId, String email, String role) {
        Date maintenant = new Date();
        Date expiration = new Date(maintenant.getTime() + expirationMinutes * 60_000);
        return Jwts.builder()
                .subject(email)
                .claim("userId", utilisateurId)
                .claim("role", role)
                .issuedAt(maintenant)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public Optional<Claims> parser(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
