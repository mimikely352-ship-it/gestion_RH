package com.sirh.evaluation.repository;

import com.sirh.evaluation.entity.ReponseEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReponseEvaluationRepository extends JpaRepository<ReponseEvaluation, Long> {
    List<ReponseEvaluation> findByEvaluationId(Long evaluationId);
}
