package com.sirh.embauche.repository;
import com.sirh.embauche.entity.Affectation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AffectationRepository extends JpaRepository<Affectation, Long> {
    Optional<Affectation> findByDossierIdAndActuelleTrue(Long id);
    List<Affectation> findByDossierIdOrderByIdAsc(Long id);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"dossier", "poste"})
    List<Affectation> findByDossierIdInOrderByIdAsc(List<Long> ids);
}
