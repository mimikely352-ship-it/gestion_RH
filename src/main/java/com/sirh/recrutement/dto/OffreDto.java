package com.sirh.recrutement.dto;

import com.sirh.recrutement.entity.OffreEmploi;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class OffreDto {
    private final Long id;
    private final String titre;
    private final Long entrepriseId;
    private final String entrepriseNom;
    private final String description;
    private final String posteIntitule;
    private final int nbPostes;
    private final String statut;
    private final LocalDate dateLimite;
    private final java.util.List<String> piecesRequises;

    public OffreDto(OffreEmploi o) {
        this.id = o.getId();
        this.titre = o.getTitre();
        this.entrepriseId = o.getEntreprise() == null ? null : o.getEntreprise().getId();
        this.entrepriseNom = o.getEntrepriseNom();
        this.description = o.getDescription();
        this.posteIntitule = o.getPosteIntitule();
        this.nbPostes = o.getNbPostes();
        this.statut = o.getStatut().name();
        this.dateLimite = o.getDateLimite();
        this.piecesRequises = o.getListePiecesRequises();
    }
}
