package com.sirh.embauche.entity;

import com.sirh.commun.enums.StatutTache;
import com.sirh.commun.enums.TacheOnboarding;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Une étape de la checklist d'intégration (badge, matériel, inscription paie...). */
@Entity @Table(name = "onboarding_tache")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OnboardingTache {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "onboarding_id", nullable = false) private Onboarding onboarding;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TacheOnboarding code;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatutTache statut;
    @Column(name = "fait_le") private LocalDateTime faitLe;
    @Column(name = "fait_par", length = 150) private String faitPar;
}
