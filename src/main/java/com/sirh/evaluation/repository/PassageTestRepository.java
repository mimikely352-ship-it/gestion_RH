package com.sirh.evaluation.repository;

import com.sirh.evaluation.entity.PassageTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PassageTestRepository extends JpaRepository<PassageTest, Long> {
    List<PassageTest> findByCandidatureId(Long candidatureId);
}
