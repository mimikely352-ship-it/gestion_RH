package com.sirh.demande.repository;

import com.sirh.demande.entity.ValidationDemande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationDemandeRepository extends JpaRepository<ValidationDemande, Long> {
    List<ValidationDemande> findByDemandeIdOrderByDateValidationAsc(Long demandeId);
}
