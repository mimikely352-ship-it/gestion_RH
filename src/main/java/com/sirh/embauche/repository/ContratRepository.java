package com.sirh.embauche.repository;

import com.sirh.commun.enums.StatutContrat;
import com.sirh.embauche.entity.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat, Long> {
    List<Contrat> findByDossierIdOrderByDateDebutDescIdDesc(Long dossierId);
    Optional<Contrat> findFirstByDossierIdAndStatut(Long dossierId, StatutContrat statut);
    List<Contrat> findByDossierIdInAndStatut(java.util.Collection<Long> dossierIds, StatutContrat statut);
}
