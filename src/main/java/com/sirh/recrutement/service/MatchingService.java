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
 * Calcule un score indicatif sur 100 : 60 points pour le diplôme et 40 pour
 * l'expérience. La décision de présélection appartient aux RH.
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

    /** Compatibilité avec l'ancien calcul ; ne déclenche aucune décision. */
    @Deprecated
    public boolean estAccepteAutomatiquement(BigDecimal score, OffreEmploi offre) {
        return score.compareTo(BigDecimal.valueOf(offre.getSeuilAcceptation())) >= 0;
    }

    private BigDecimal scoreDiplome(Candidat candidat, OffreEmploi offre) {
        // Elle récupère les identifiants des diplômes exigés par l’offre
        Set<Long> diplomesRequisIds = offre.getDiplomesRequis().stream()
                .map(d -> d.getId())
                .collect(Collectors.toSet());
        // S’il n’y a aucun diplôme exigé, elle donne les 60 points
        if (diplomesRequisIds.isEmpty()) {
            return POIDS_DIPLOME; // aucune exigence -> plein score par défaut
        }
        // Sinon, elle regarde si le candidat possède au moins un des diplômes exigés
        boolean possedeUnDiplomeRequis = candidat.getDiplomes().stream()
                .anyMatch(d -> diplomesRequisIds.contains(d.getId()));
        // Elle donne alors 60 points s’il en possède un, ou 0 point dans le cas contraire
        return possedeUnDiplomeRequis ? POIDS_DIPLOME : BigDecimal.ZERO;
    }

    private BigDecimal scoreExperience(Candidat candidat, OffreEmploi offre) {
        // prendre le Experience Min d un orffre 
        Integer experienceMinMois = offre.getExperienceMinMois();
        if (experienceMinMois == null || experienceMinMois <= 0) {
            return POIDS_EXPERIENCE; // pas d'exigence -> plein score par défaut
        }
        // prendre le mois d expereince candidat
        long totalMoisCandidat = experienceRepository.findByCandidatId(candidat.getId()).stream()
                .mapToLong(Experience::dureeEnMois)
                .sum();
        BigDecimal ratio = BigDecimal.valueOf(totalMoisCandidat)
                .divide(BigDecimal.valueOf(experienceMinMois), 4, RoundingMode.HALF_UP);
        BigDecimal ratioPlafonne = ratio.min(BigDecimal.ONE);
        return ratioPlafonne.multiply(POIDS_EXPERIENCE);
        // l’offre demande 6 mois d’expérience. Avec 3 mois, le ratio vaut 3 ÷ 6 = 0,5, donc le candidat reçoit 0,5 × 40 = 20 points
    }
}
