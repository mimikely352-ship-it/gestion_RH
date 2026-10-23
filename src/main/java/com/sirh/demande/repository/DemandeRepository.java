package com.sirh.demande.repository;

import com.sirh.commun.enums.StatutDemande;
import com.sirh.demande.entity.Demande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeRepository extends JpaRepository<Demande, Long> {
    List<Demande> findByAuteurIdIn(List<Long> ids);
    @org.springframework.data.jpa.repository.Query("select d from Demande d where (d.type = com.sirh.commun.enums.TypeDemande.TRANSFERT and d.statut in (com.sirh.commun.enums.StatutDemande.SOUMISE, com.sirh.commun.enums.StatutDemande.VALIDEE_CHEF, com.sirh.commun.enums.StatutDemande.VALIDEE_DIRECTION)) or (d.type <> com.sirh.commun.enums.TypeDemande.TRANSFERT and d.statut = com.sirh.commun.enums.StatutDemande.VALIDEE_DIRECTION)")
    List<Demande> aValiderParRh();
    @org.springframework.data.jpa.repository.Query("select d from Demande d where d.statut = com.sirh.commun.enums.StatutDemande.VALIDEE_DG or (d.type = com.sirh.commun.enums.TypeDemande.TRANSFERT and d.statut = com.sirh.commun.enums.StatutDemande.VALIDEE_RH)")
    List<Demande> aExecuterParRh();
    List<Demande> findByAuteurId(Long auteurId);
    List<Demande> findByStatut(StatutDemande statut);
}
