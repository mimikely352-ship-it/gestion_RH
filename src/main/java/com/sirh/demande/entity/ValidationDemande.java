package com.sirh.demande.entity;

import com.sirh.commun.enums.DecisionValidation;
import com.sirh.commun.enums.EtapeValidation;
import com.sirh.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "validation_demande")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationDemande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EtapeValidation etape;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "valideur_id", nullable = false)
    private Utilisateur valideur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DecisionValidation decision;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_validation", nullable = false)
    private LocalDateTime dateValidation;

    @PrePersist
    void prePersist() {
        if (dateValidation == null) {
            dateValidation = LocalDateTime.now();
        }
    }

    @Transient
    public String getValideurEmail() {
        return valideur == null ? "" : valideur.getEmail();
    }
}
