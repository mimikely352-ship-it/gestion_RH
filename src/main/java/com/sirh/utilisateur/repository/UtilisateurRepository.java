package com.sirh.utilisateur.repository;

import com.sirh.commun.enums.Role;
import com.sirh.utilisateur.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Utilisateur u where u.id = :id")
    Optional<Utilisateur> verrouiller(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Utilisateur> findByRole(Role role);
    List<Utilisateur> findByActifTrueOrderByEmailAsc();
}
