package com.sirh.presence.repository;
import com.sirh.presence.entity.PointageJour;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.*;
public interface PointageJourRepository extends JpaRepository<PointageJour,Long> {
    Optional<PointageJour> findFirstByDossierIdAndSortieIsNullOrderByJourAsc(Long dossierId);
    Optional<PointageJour> findByDossierIdAndJour(Long dossierId, LocalDate jour);
    List<PointageJour> findByDossierIdAndJourBetweenOrderByJourAsc(Long dossierId, LocalDate debut, LocalDate fin);
}
