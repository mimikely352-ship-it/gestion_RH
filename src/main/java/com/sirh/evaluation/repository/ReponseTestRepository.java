package com.sirh.evaluation.repository;

import com.sirh.evaluation.entity.ReponseTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReponseTestRepository extends JpaRepository<ReponseTest, Long> {
    List<ReponseTest> findByPassageId(Long passageId);
}
