package com.sirh.embauche.repository;

import com.sirh.embauche.entity.Onboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OnboardingRepository extends JpaRepository<Onboarding, Long> {
    Optional<Onboarding> findByDossierId(Long dossierId);
    List<Onboarding> findByDossierIdIn(Collection<Long> dossierIds);
}
