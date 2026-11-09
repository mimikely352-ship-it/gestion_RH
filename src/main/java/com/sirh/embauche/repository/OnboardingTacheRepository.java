package com.sirh.embauche.repository;

import com.sirh.embauche.entity.OnboardingTache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OnboardingTacheRepository extends JpaRepository<OnboardingTache, Long> {
    List<OnboardingTache> findByOnboardingId(Long onboardingId);
    List<OnboardingTache> findByOnboardingIdIn(Collection<Long> onboardingIds);
}
