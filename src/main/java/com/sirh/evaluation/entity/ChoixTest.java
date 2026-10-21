package com.sirh.evaluation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "choix_test")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChoixTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionTest question;

    @Column(nullable = false, length = 255)
    private String libelle;

    @Column(name = "est_correct", nullable = false)
    @Builder.Default
    private boolean estCorrect = false;
}
