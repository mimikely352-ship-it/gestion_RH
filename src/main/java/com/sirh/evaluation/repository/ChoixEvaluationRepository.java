package com.sirh.evaluation.repository;

import com.sirh.evaluation.entity.ChoixEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChoixEvaluationRepository extends JpaRepository<ChoixEvaluation, Long> {
    List<ChoixEvaluation> findByQuestionId(Long questionId);
}
