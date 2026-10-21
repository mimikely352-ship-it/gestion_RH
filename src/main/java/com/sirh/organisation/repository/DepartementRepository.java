package com.sirh.organisation.repository;

import com.sirh.organisation.entity.Departement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartementRepository extends JpaRepository<Departement, Long> {
    List<Departement> findByDirectionId(Long directionId);
}
