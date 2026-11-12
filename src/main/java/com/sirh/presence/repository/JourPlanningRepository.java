package com.sirh.presence.repository;
import com.sirh.presence.entity.JourPlanning;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.*;
public interface JourPlanningRepository extends JpaRepository<JourPlanning,Long> {
    List<JourPlanning> findByDossierIdAndJourBetween(Long dossierId, LocalDate debut, LocalDate fin);
    Optional<JourPlanning> findByDossierIdAndJour(Long dossierId, LocalDate jour);
}
