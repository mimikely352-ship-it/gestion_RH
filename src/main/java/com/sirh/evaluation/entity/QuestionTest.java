package com.sirh.evaluation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question_test")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enonce;

    @Column(nullable = false)
    @Builder.Default
    private int ordre = 0;

    /** Lecture seule côté JSP (affichage des choix) : géré via ChoixTestRepository côté écriture. */
    @Builder.Default
    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<ChoixTest> choix = new ArrayList<>();
}
