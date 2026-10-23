package com.sirh.evaluation.repository;

import com.sirh.evaluation.entity.PassageTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PassageTestRepository extends JpaRepository<PassageTest, Long> {
    boolean existsByCandidatureIdAndTestId(Long candidatureId, Long testId);
    boolean existsByTestId(Long testId);
    List<PassageTest> findByCandidatureId(Long candidatureId);
}
