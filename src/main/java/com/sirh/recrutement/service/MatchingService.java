package com.sirh.recrutement.service;

import com.sirh.candidat.entity.Candidat;
import com.sirh.candidat.entity.Experience;
import com.sirh.candidat.repository.ExperienceRepository;
import com.sirh.recrutement.entity.OffreEmploi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcule le score de correspondance candidat/offre (sur 100) utilisé pour l'acceptation
 * automatique à l'entretien : 60 points pour le diplôme, 40 pour l'expérience. Voir le plan
 * d'implémentation §4 pour le détail de la règle.
 */
@Service
@RequiredArgsConstructor
public class MatchingService {

    private static final BigDecimal POIDS_DIPLOME = BigDecimal.valueOf(60);
    private static final BigDecimal POIDS_EXPERIENCE = BigDecimal.valueOf(40);

    private final ExperienceRepository experienceRepository;

    public BigDecimal calculerScore(Candidat candidat, OffreEmploi offre) {
        BigDecimal scoreDiplome = scoreDiplome(candidat, offre);
        BigDecimal scoreExperience = scoreExperience(candidat, offre);
        return scoreDiplome.add(scoreExperience).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean estAccepteAutomatiquement(BigDecimal score, OffreEmploi offre) {
        return score.compareTo(BigDecimal.valueOf(offre.getSeuilAcceptation())) >= 0;
    }

    private BigDecimal scoreDiplome(Candidat candidat, OffreEmploi offre) {
        Set<Long> diplomesRequisIds = offre.getDiplomesRequis().stream()
                .map(d -> d.getId())
                .collect(Collectors.toSet());
        if (diplomesRequisIds.isEmpty()) {
            return POIDS_DIPLOME; // aucune exigence -> plein score par défaut
        }
        boolean possedeUnDiplomeRequis = candidat.getDiplomes().stream()
                .anyMatch(d -> diplomesRequisIds.contains(d.getId()));
        return possedeUnDiplomeRequis ? POIDS_DIPLOME : BigDecimal.ZERO;
    }

    private BigDecimal scoreExperience(Candidat candidat, OffreEmploi offre) {
        Integer experienceMinMois = offre.getExperienceMinMois();
        if (experienceMinMois == null || experienceMinMois <= 0) {
            return POIDS_EXPERIENCE; // pas d'exigence -> plein score par défaut
        }
        long totalMoisCandidat = experienceRepository.findByCandidatId(candidat.getId()).stream()
                .mapToLong(Experience::dureeEnMois)
                .sum();
        BigDecimal ratio = BigDecimal.valueOf(totalMoisCandidat)
                .divide(BigDecimal.valueOf(experienceMinMois), 4, RoundingMode.HALF_UP);
        BigDecimal ratioPlafonne = ratio.min(BigDecimal.ONE);
        return ratioPlafonne.multiply(POIDS_EXPERIENCE);
    }
}
