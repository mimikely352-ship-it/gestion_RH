package com.sirh.evaluation.repository;

import com.sirh.evaluation.entity.ChoixTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChoixTestRepository extends JpaRepository<ChoixTest, Long> {
    List<ChoixTest> findByQuestionId(Long questionId);
}
