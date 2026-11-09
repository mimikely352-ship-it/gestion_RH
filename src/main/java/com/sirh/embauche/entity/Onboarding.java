package com.sirh.embauche.entity;

import com.sirh.commun.enums.StatutOnboarding;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Workflow d'intégration d'un employé ; ses étapes sont les OnboardingTache. */
@Entity @Table(name = "onboarding")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Onboarding {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "dossier_id", nullable = false, unique = true) private Long dossierId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatutOnboarding statut;
    @Column(name = "date_debut", nullable = false) private LocalDate dateDebut;
    @Column(name = "termine_le") private LocalDateTime termineLe;
}
