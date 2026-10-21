package com.sirh.ordre.entity;

import com.sirh.commun.enums.StatutOrdre;
import com.sirh.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ordre")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ordre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dg_id", nullable = false)
    private Utilisateur dg;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rh_id", nullable = false)
    private Utilisateur rh;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "delai_jours", nullable = false)
    @Builder.Default
    private int delaiJours = 7;

    @Column(name = "date_emission", nullable = false)
    private LocalDateTime dateEmission;

    @Column(name = "date_limite", nullable = false)
    private LocalDateTime dateLimite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private StatutOrdre statut = StatutOrdre.EMIS;

    @PrePersist
    void prePersist() {
        if (dateEmission == null) {
            dateEmission = LocalDateTime.now();
        }
        if (dateLimite == null) {
            dateLimite = dateEmission.plusDays(delaiJours);
        }
    }

    @Transient
    public String getRhEmail() {
        return rh == null ? "" : rh.getEmail();
    }

    @Transient
    public String getDgEmail() {
        return dg == null ? "" : dg.getEmail();
    }
}
