package com.sirh.evaluation.entity;

import com.sirh.commun.enums.FormatEvaluation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question_evaluation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enonce;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FormatEvaluation type;

    @Column(nullable = false)
    @Builder.Default
    private int ordre = 0;

    /** Lecture seule côté JSP (affichage des choix si QCM). */
    @Builder.Default
    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<ChoixEvaluation> choix = new ArrayList<>();
}
