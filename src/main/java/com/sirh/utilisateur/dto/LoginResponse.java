package com.sirh.utilisateur.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private final String token;
    private final Long utilisateurId;
    private final String email;
    private final String role;
}
