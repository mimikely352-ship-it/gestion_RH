package com.sirh.recrutement.dto;

import com.sirh.recrutement.entity.Candidature;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class CandidatureDto {
    private final Long id;
    private final String offreTitre;
    private final String candidatNomComplet;
    private final String statut;
    private final BigDecimal scoreMatching;
    private final LocalDateTime dateCandidature;

    public CandidatureDto(Candidature c) {
        this.id = c.getId();
        this.offreTitre = c.getOffreTitre();
        this.candidatNomComplet = c.getCandidatNomComplet();
        this.statut = c.getStatut().name();
        this.scoreMatching = c.getScoreMatching();
        this.dateCandidature = c.getDateCandidature();
    }
}
