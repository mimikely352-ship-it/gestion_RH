package com.sirh.embauche.repository;

import com.sirh.commun.enums.StatutDepart;
import com.sirh.embauche.entity.Depart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DepartRepository extends JpaRepository<Depart, Long> {
    List<Depart> findByDossierIdOrderByIdDesc(Long dossierId);
    Optional<Depart> findFirstByDossierIdAndStatut(Long dossierId, StatutDepart statut);
    List<Depart> findByDossierIdInAndStatut(Collection<Long> dossierIds, StatutDepart statut);
    @EntityGraph(attributePaths = {"dossier"})
    List<Depart> findTop100ByStatutInOrderByDateEffetDescIdDesc(Collection<StatutDepart> statuts);
}
