package com.sirh.demande.entity;

import com.sirh.commun.enums.StatutDemande;
import com.sirh.commun.enums.TypeDemande;
import com.sirh.organisation.entity.Poste;
import com.sirh.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "demande")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Demande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version private Long version;
    @Column(name = "affectation_origine_id") private Long affectationOrigineId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affectation_origine_id", insertable = false, updatable = false)
    private com.sirh.embauche.entity.Affectation affectationOrigine;
    @Column(name = "date_effet") private java.time.LocalDate dateEffet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeDemande type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auteur_id", nullable = false)
    private Utilisateur auteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_cible_id")
    private Poste posteCible;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutDemande statut = StatutDemande.SOUMISE;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_limite_execution")
    private LocalDateTime dateLimiteExecution;

    @Column(name = "date_execution")
    private LocalDateTime dateExecution;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

    @Transient
    public String getAuteurEmail() {
        return auteur == null ? "" : auteur.getEmail();
    }
}
