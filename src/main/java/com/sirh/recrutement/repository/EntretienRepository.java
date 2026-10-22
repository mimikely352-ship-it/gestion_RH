package com.sirh.recrutement.repository;

import com.sirh.recrutement.entity.Entretien;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntretienRepository extends JpaRepository<Entretien, Long> {
    List<Entretien> findByCandidature_Id(Long candidatureId);
}
