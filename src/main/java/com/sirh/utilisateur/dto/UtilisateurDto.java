package com.sirh.utilisateur.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UtilisateurDto {
    private final Long id;
    private final String email;
    private final String role;
    private final Long serviceId;
}
