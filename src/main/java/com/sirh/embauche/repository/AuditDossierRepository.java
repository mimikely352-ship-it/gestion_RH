package com.sirh.embauche.repository;

import com.sirh.embauche.entity.AuditDossier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditDossierRepository extends JpaRepository<AuditDossier, Long> {
    List<AuditDossier> findTop100ByDossierIdOrderByHorodatageDescIdDesc(Long dossierId);
}
