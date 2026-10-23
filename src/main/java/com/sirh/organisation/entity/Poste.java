package com.sirh.organisation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "poste")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Builder.Default
    @Column(name = "mobilite_ouverte", nullable = false)
    private boolean mobiliteOuverte = true;

    @Column(nullable = false, length = 150)
    private String intitule;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 150)
    private String categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;
}
