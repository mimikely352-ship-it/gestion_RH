package com.sirh.ordre.dto;

import com.sirh.ordre.entity.Ordre;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrdreDto {
    private final Long id;
    private final String description;
    private final String dgEmail;
    private final String rhEmail;
    private final int delaiJours;
    private final String statut;
    private final LocalDateTime dateEmission;
    private final LocalDateTime dateLimite;

    public OrdreDto(Ordre o) {
        this.id = o.getId();
        this.description = o.getDescription();
        this.dgEmail = o.getDgEmail();
        this.rhEmail = o.getRhEmail();
        this.delaiJours = o.getDelaiJours();
        this.statut = o.getStatut().name();
        this.dateEmission = o.getDateEmission();
        this.dateLimite = o.getDateLimite();
    }
}
