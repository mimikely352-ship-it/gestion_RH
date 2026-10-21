package com.sirh.candidat.repository;

import com.sirh.candidat.entity.Candidat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidatRepository extends JpaRepository<Candidat, Long> {
    Optional<Candidat> findByUtilisateurId(Long utilisateurId);
    Optional<Candidat> findByUtilisateurEmail(String email);
}
