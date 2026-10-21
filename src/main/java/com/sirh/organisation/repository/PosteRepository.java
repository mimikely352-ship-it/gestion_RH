package com.sirh.organisation.repository;

import com.sirh.organisation.entity.Poste;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PosteRepository extends JpaRepository<Poste, Long> {
    List<Poste> findBySectionId(Long sectionId);
}
