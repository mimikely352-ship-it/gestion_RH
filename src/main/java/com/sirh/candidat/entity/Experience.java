package com.sirh.candidat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "experience")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidat_id", nullable = false)
    private Candidat candidat;

    @Column(nullable = false, length = 150)
    private String intitule;

    @Column(length = 150)
    private String entreprise;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin; // null = poste actuel

    @Column(columnDefinition = "TEXT")
    private String description;

    @Transient
    public long dureeEnMois() {
        LocalDate fin = dateFin != null ? dateFin : LocalDate.now();
        return Period.between(dateDebut, fin).toTotalMonths();
    }
}
