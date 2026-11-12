package com.sirh.presence.repository;
import com.sirh.presence.entity.DemandeConge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.*;
public interface DemandeCongeRepository extends JpaRepository<DemandeConge,Long> {
    @org.springframework.data.jpa.repository.Query("select d.dossierId from DemandeConge d where d.id=:id")
    Optional<Long> dossierId(@org.springframework.data.repository.query.Param("id") Long id);
    List<DemandeConge> findByDossierIdOrderBySoumiseLeDesc(Long dossierId);
    List<DemandeConge> findByDossierIdAndDebutLessThanEqualAndFinGreaterThanEqual(Long dossierId, LocalDate fin, LocalDate debut);
    List<DemandeConge> findByStatutOrderBySoumiseLeAsc(com.sirh.presence.entity.StatutConge statut);
    List<DemandeConge> findByResponsableIdAndStatutOrderBySoumiseLeAsc(Long responsableId, com.sirh.presence.entity.StatutConge statut);
}
