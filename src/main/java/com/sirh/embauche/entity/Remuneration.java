package com.sirh.embauche.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Ligne de l'historique de rémunération : jamais modifiée, une révision ajoute une ligne. */
@Entity @Table(name = "remuneration_employe")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Remuneration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "dossier_id", nullable = false) private Long dossierId;
    @Column(name = "contrat_id") private Long contratId;
    @Column(name = "date_effet", nullable = false) private LocalDate dateEffet;
    @Column(name = "salaire_brut", nullable = false, precision = 12, scale = 2) private BigDecimal salaireBrut;
    @Column(nullable = false, length = 150) private String motif;
    @Column(name = "cree_le", nullable = false) private LocalDateTime creeLe;
    @Column(name = "cree_par", nullable = false, length = 150) private String creePar;
}
