package com.sirh.evaluation.repository;

import com.sirh.evaluation.entity.QuestionTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionTestRepository extends JpaRepository<QuestionTest, Long> {
    List<QuestionTest> findByTestIdOrderByOrdreAsc(Long testId);
}
