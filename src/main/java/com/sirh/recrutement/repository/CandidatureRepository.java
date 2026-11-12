package com.sirh.recrutement.repository;

import com.sirh.recrutement.entity.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidatureRepository extends JpaRepository<Candidature, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Candidature c where c.id = :id")
    java.util.Optional<Candidature> verrouiller(@org.springframework.data.repository.query.Param("id") Long id);
    List<Candidature> findByCandidatUtilisateurIdAndStatut(Long utilisateurId, com.sirh.commun.enums.StatutCandidature statut);
    List<Candidature> findByCandidatId(Long candidatId);
    List<Candidature> findByOffreId(Long offreId);
    long countByOffreIdAndStatut(Long offreId, com.sirh.commun.enums.StatutCandidature statut);
    long countByOffreIdAndPropositionStatut(Long offreId, com.sirh.commun.enums.StatutProposition statut);
    long countByOffreIdAndStatutAndPropositionStatut(Long offreId, com.sirh.commun.enums.StatutCandidature statut,
                                                     com.sirh.commun.enums.StatutProposition propositionStatut);
    boolean existsByOffreIdAndCandidatId(Long offreId, Long candidatId);
    boolean existsByCandidatIdAndOffreEntrepriseId(Long candidatId, Long entrepriseId);
    /** Somme des salaires mensuels engagés (propositions en attente ou acceptées) sur un besoin. */
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(c.propositionSalaire), 0) from Candidature c "
            + "where c.offre.demandeOrigine.id = :demandeId and c.id <> :exclue "
            + "and c.propositionStatut in (com.sirh.commun.enums.StatutProposition.EN_ATTENTE, com.sirh.commun.enums.StatutProposition.ACCEPTEE)")
    java.math.BigDecimal salairesMensuelsEngages(@org.springframework.data.repository.query.Param("demandeId") Long demandeId,
                                                 @org.springframework.data.repository.query.Param("exclue") Long exclue);
}
