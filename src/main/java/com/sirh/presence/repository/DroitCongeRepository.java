package com.sirh.presence.repository;
import com.sirh.presence.entity.DroitConge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.*;
public interface DroitCongeRepository extends JpaRepository<DroitConge,Long> {
    Optional<DroitConge> findByDossierIdAndTypeIdAndAnnee(Long dossierId, Long typeId, int annee);
}
