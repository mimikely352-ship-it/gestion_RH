package com.sirh.recrutement.dto;

import com.sirh.recrutement.entity.OffreEmploi;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class OffreDto {
    private final Long id;
    private final String titre;
    private final String description;
    private final String posteIntitule;
    private final int nbPostes;
    private final String statut;
    private final LocalDate dateLimite;

    public OffreDto(OffreEmploi o) {
        this.id = o.getId();
        this.titre = o.getTitre();
        this.description = o.getDescription();
        this.posteIntitule = o.getPosteIntitule();
        this.nbPostes = o.getNbPostes();
        this.statut = o.getStatut().name();
        this.dateLimite = o.getDateLimite();
    }
}
