package com.sirh.evaluation.repository;

import com.sirh.evaluation.entity.QuestionEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionEvaluationRepository extends JpaRepository<QuestionEvaluation, Long> {
    List<QuestionEvaluation> findByEvaluationIdOrderByOrdreAsc(Long evaluationId);
}
