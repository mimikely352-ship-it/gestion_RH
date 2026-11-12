package com.sirh.recrutement.repository;

import com.sirh.commun.enums.StatutOffre;
import com.sirh.recrutement.entity.OffreEmploi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OffreEmploiRepository extends JpaRepository<OffreEmploi, Long> {
    List<OffreEmploi> findByStatut(StatutOffre statut);
    List<OffreEmploi> findByStatutAndDateLimiteBefore(StatutOffre statut, java.time.LocalDate date);
    /** Verrou de l'offre : sérialise l'envoi des propositions sur ses postes. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from OffreEmploi o where o.id = :id")
    java.util.Optional<OffreEmploi> verrouiller(@org.springframework.data.repository.query.Param("id") Long id);
}
