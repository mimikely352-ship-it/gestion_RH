package com.sirh.recrutement.repository;

import com.sirh.recrutement.entity.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidatureRepository extends JpaRepository<Candidature, Long> {
    List<Candidature> findByCandidatId(Long candidatId);
    List<Candidature> findByOffreId(Long offreId);
    boolean existsByOffreIdAndCandidatId(Long offreId, Long candidatId);
}
