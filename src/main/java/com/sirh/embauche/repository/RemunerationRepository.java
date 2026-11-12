package com.sirh.embauche.repository;

import com.sirh.embauche.entity.Remuneration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RemunerationRepository extends JpaRepository<Remuneration, Long> {
    List<Remuneration> findByDossierIdOrderByDateEffetDescIdDesc(Long dossierId);
    /** Lignes d'un contrat annulé avant son début : elles n'ont jamais pris effet. */
    List<Remuneration> findByContratIdAndDateEffetAfter(Long contratId, java.time.LocalDate date);
}
