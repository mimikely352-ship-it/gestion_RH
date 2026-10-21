package com.sirh.evaluation.repository;

import com.sirh.evaluation.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByCandidatureId(Long candidatureId);
}
