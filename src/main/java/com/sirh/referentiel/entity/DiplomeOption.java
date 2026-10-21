package com.sirh.referentiel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "diplome_option")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiplomeOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diplome_filiere_id", nullable = false)
    private DiplomeFiliere filiere;

    @Column(name = "option_nom", nullable = false, length = 150)
    private String optionNom;
}
