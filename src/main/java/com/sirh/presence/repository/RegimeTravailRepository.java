package com.sirh.presence.repository;
import com.sirh.presence.entity.RegimeTravail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.*;
public interface RegimeTravailRepository extends JpaRepository<RegimeTravail,Long> {
    List<RegimeTravail> findByDossierIdOrderByDepuisDesc(Long dossierId);
    Optional<RegimeTravail> findByDossierIdAndDepuis(Long dossierId, LocalDate depuis);
}
