package com.sirh.evaluation.entity;

import com.sirh.commun.enums.Decision;
import com.sirh.commun.enums.FormatEvaluation;
import com.sirh.recrutement.entity.Candidature;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidature_id", nullable = false)
    private Candidature candidature;

    @Column(nullable = false, length = 150)
    private String titre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FormatEvaluation format;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_rh", nullable = false, length = 15)
    @Builder.Default
    private Decision decisionRh = Decision.EN_ATTENTE;

    @Column(name = "date_evaluation")
    private LocalDateTime dateEvaluation;
}
