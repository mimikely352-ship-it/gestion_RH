package com.sirh.demande.dto;

import com.sirh.demande.entity.Demande;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DemandeDto {
    private final Long id;
    private final String type;
    private final String auteurEmail;
    private final String description;
    private final String statut;
    private final LocalDateTime dateCreation;
    private final LocalDateTime dateLimiteExecution;

    public DemandeDto(Demande d) {
        this.id = d.getId();
        this.type = d.getType().name();
        this.auteurEmail = d.getAuteurEmail();
        this.description = d.getDescription();
        this.statut = d.getStatut().name();
        this.dateCreation = d.getDateCreation();
        this.dateLimiteExecution = d.getDateLimiteExecution();
    }
}
