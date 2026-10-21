package com.sirh.recrutement.repository;

import com.sirh.commun.enums.StatutOffre;
import com.sirh.recrutement.entity.OffreEmploi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OffreEmploiRepository extends JpaRepository<OffreEmploi, Long> {
    List<OffreEmploi> findByStatut(StatutOffre statut);
}
