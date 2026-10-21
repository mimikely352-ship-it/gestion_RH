package com.sirh.ordre.repository;

import com.sirh.ordre.entity.Ordre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdreRepository extends JpaRepository<Ordre, Long> {
    List<Ordre> findByRhId(Long rhId);
}
