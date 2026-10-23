package com.sirh.embauche.repository;
import com.sirh.embauche.entity.DossierEmploye;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface DossierEmployeRepository extends JpaRepository<DossierEmploye, Long> {
    @EntityGraph(attributePaths = {"utilisateur"})
    org.springframework.data.domain.Page<DossierEmploye> findByUtilisateurRoleAndUtilisateurActifTrue(
            com.sirh.commun.enums.Role role, org.springframework.data.domain.Pageable page);
    long countByUtilisateurRoleAndUtilisateurActifTrue(com.sirh.commun.enums.Role role);
    Optional<DossierEmploye> findByUtilisateurId(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DossierEmploye d where d.utilisateur.id = :id")
    Optional<DossierEmploye> verrouillerParUtilisateur(@org.springframework.data.repository.query.Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DossierEmploye d where d.id = :id")
    Optional<DossierEmploye> verrouillerParId(@org.springframework.data.repository.query.Param("id") Long id);
}
