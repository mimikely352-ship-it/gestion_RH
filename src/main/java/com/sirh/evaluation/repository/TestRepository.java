package com.sirh.evaluation.repository;

import com.sirh.evaluation.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByOffreIdOrderByNiveauAsc(Long offreId);
}
