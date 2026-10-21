package com.sirh.recrutement.entity;

import com.sirh.candidat.entity.Candidat;
import com.sirh.candidat.entity.Document;
import com.sirh.commun.enums.StatutCandidature;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidature", uniqueConstraints = @UniqueConstraint(columnNames = {"offre_id", "candidat_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_id", nullable = false)
    private OffreEmploi offre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidat_id", nullable = false)
    private Candidat candidat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_document_id")
    private Document cvDocument;

    @Column(name = "score_matching", precision = 5, scale = 2)
    private BigDecimal scoreMatching;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private StatutCandidature statut = StatutCandidature.RECUE;

    @Column(name = "date_candidature", nullable = false)
    private LocalDateTime dateCandidature;

    @PrePersist
    void prePersist() {
        if (dateCandidature == null) {
            dateCandidature = LocalDateTime.now();
        }
    }

    /** Étape de la timeline affichée côté candidat (0 = reçue, 4 = décision finale). */
    @Transient
    public int getEtapeOrdinale() {
        return switch (statut) {
            case RECUE, NON_RETENUE -> 0;
            case ACCEPTEE_ENTRETIEN, EN_ENTRETIEN, REFUSEE_ENTRETIEN -> 1;
            case EN_TEST -> 2;
            case EN_EVALUATION, REFUSEE -> 3;
            case RETENUE, EMBAUCHE -> 4;
        };
    }

    @Transient
    public boolean isRefusee() {
        return statut == StatutCandidature.NON_RETENUE
                || statut == StatutCandidature.REFUSEE_ENTRETIEN
                || statut == StatutCandidature.REFUSEE;
    }

    @Transient
    public String getCandidatNomComplet() {
        return candidat == null ? "" : candidat.getPrenom() + " " + candidat.getNom();
    }

    @Transient
    public String getOffreTitre() {
        return offre == null ? "" : offre.getTitre();
    }
}
