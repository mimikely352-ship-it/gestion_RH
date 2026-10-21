package com.sirh.evaluation.entity;

import com.sirh.commun.enums.DecisionTest;
import com.sirh.recrutement.entity.Candidature;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "passage_test", uniqueConstraints = @UniqueConstraint(columnNames = {"candidature_id", "test_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassageTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidature_id", nullable = false)
    private Candidature candidature;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(precision = 5, scale = 2)
    private BigDecimal note;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_rh", nullable = false, length = 15)
    @Builder.Default
    private DecisionTest decisionRh = DecisionTest.EN_ATTENTE;

    @Column(name = "date_passage")
    private LocalDateTime datePassage;
}
