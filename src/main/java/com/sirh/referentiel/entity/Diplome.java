package com.sirh.referentiel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Diplôme = croisement d'un niveau et d'une option (ex : Licence en Informatique - Développement). */
@Entity
@Table(name = "diplome", uniqueConstraints = @UniqueConstraint(columnNames = {"niveau_id", "diplome_option_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diplome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "niveau_id", nullable = false)
    private NiveauDiplome niveau;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diplome_option_id", nullable = false)
    private DiplomeOption option;

    @Transient
    public String getLibelle() {
        if (niveau == null || option == null) {
            return "";
        }
        return niveau.getNiveau() + " en " + option.getFiliere().getFiliere() + " - " + option.getOptionNom();
    }
}
