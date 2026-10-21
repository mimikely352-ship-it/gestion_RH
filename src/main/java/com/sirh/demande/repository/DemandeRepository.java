package com.sirh.demande.repository;

import com.sirh.commun.enums.StatutDemande;
import com.sirh.demande.entity.Demande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeRepository extends JpaRepository<Demande, Long> {
    List<Demande> findByAuteurId(Long auteurId);
    List<Demande> findByStatut(StatutDemande statut);
}
