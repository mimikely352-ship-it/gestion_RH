package com.sirh.candidat.repository;

import com.sirh.candidat.entity.Candidat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidatRepository extends JpaRepository<Candidat, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Candidat c where c.id = :id")
    Optional<Candidat> verrouiller(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<Candidat> findByUtilisateurId(Long utilisateurId);
    Optional<Candidat> findByUtilisateurEmail(String email);
}
