package com.sirh.recrutement.entity;

import com.sirh.commun.enums.Decision;
import com.sirh.commun.enums.StatutEntretien;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "entretien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entretien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidature_id", nullable = false)
    private Candidature candidature;

    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;

    @Column(length = 150)
    private String lieu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private StatutEntretien statut = StatutEntretien.PLANIFIE;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_rh", nullable = false, length = 15)
    @Builder.Default
    private Decision decisionRh = Decision.EN_ATTENTE;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Transient
    public Long getCandidatureId() {
        return candidature == null ? null : candidature.getId();
    }

    @Transient
    public String getCandidatNomComplet() {
        return candidature == null ? "" : candidature.getCandidatNomComplet();
    }

    @Transient
    public String getOffreTitre() {
        return candidature == null ? "" : candidature.getOffreTitre();
    }
}
