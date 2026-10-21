package com.sirh.referentiel.repository;

import com.sirh.referentiel.entity.DiplomeOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiplomeOptionRepository extends JpaRepository<DiplomeOption, Long> {
    List<DiplomeOption> findByFiliereId(Long filiereId);
}
