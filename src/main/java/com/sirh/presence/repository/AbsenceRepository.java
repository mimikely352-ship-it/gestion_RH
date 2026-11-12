package com.sirh.presence.repository;
import com.sirh.presence.entity.Absence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.*;
public interface AbsenceRepository extends JpaRepository<Absence,Long> {
    @org.springframework.data.jpa.repository.Query("select a.dossierId from Absence a where a.id=:id")
    Optional<Long> dossierId(@org.springframework.data.repository.query.Param("id") Long id);
    List<Absence> findByDossierIdAndActiveTrueAndDebutLessThanEqualAndFinGreaterThanEqual(Long dossierId, LocalDate fin, LocalDate debut);
    Optional<Absence> findByDemandeId(Long demandeId);
}
