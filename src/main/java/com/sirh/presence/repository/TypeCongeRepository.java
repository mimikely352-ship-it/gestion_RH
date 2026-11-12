package com.sirh.presence.repository;
import com.sirh.presence.entity.TypeConge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.*;
public interface TypeCongeRepository extends JpaRepository<TypeConge,Long> {
    boolean existsByNomIgnoreCase(String nom);
    List<TypeConge> findByActifTrueOrderByNomAsc();
}
