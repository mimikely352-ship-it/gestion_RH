package com.sirh.recrutement.service;

import com.sirh.candidat.entity.Candidat;
import com.sirh.candidat.entity.Experience;
import com.sirh.candidat.repository.ExperienceRepository;
import com.sirh.recrutement.entity.OffreEmploi;
import com.sirh.referentiel.entity.Diplome;
import com.sirh.referentiel.entity.DiplomeOption;
import com.sirh.referentiel.entity.NiveauDiplome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchingServiceTest {

    private ExperienceRepository experienceRepository;
    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        experienceRepository = Mockito.mock(ExperienceRepository.class);
        matchingService = new MatchingService(experienceRepository);
    }

    private Diplome diplome(long id) {
        Diplome d = new Diplome();
        d.setId(id);
        d.setNiveau(NiveauDiplome.builder().id(1L).niveau("Licence").build());
        d.setOption(new DiplomeOption());
        return d;
    }

    @Test
    void scoreMaximalQuandAucuneExigence() {
        Candidat candidat = Candidat.builder().id(1L).diplomes(new HashSet<>()).build();
        OffreEmploi offre = OffreEmploi.builder().id(1L).seuilAcceptation(60).diplomesRequis(Set.of()).build();
        Mockito.when(experienceRepository.findByCandidatId(1L)).thenReturn(List.of());

        BigDecimal score = matchingService.calculerScore(candidat, offre);

        assertEquals(new BigDecimal("100.00"), score);
        assertTrue(matchingService.estAccepteAutomatiquement(score, offre));
    }

    @Test
    void scoreNulQuandDiplomeRequisAbsentEtAucuneExperience() {
        Diplome requis = diplome(42L);
        Candidat candidat = Candidat.builder().id(1L).diplomes(new HashSet<>()).build();
        OffreEmploi offre = OffreEmploi.builder().id(1L).seuilAcceptation(60)
                .experienceMinMois(24).diplomesRequis(Set.of(requis)).build();
        Mockito.when(experienceRepository.findByCandidatId(1L)).thenReturn(List.of());

        BigDecimal score = matchingService.calculerScore(candidat, offre);

        assertEquals(new BigDecimal("0.00"), score);
        assertFalse(matchingService.estAccepteAutomatiquement(score, offre));
    }

    @Test
    void scorePartielProportionnelALExperience() {
        Diplome requis = diplome(42L);
        Candidat candidat = Candidat.builder().id(1L).diplomes(new HashSet<>(Set.of(requis))).build();
        OffreEmploi offre = OffreEmploi.builder().id(1L).seuilAcceptation(60)
                .experienceMinMois(24).diplomesRequis(Set.of(requis)).build();
        Experience experience = Experience.builder()
                .dateDebut(LocalDate.now().minusMonths(12)).dateFin(LocalDate.now()).build();
        Mockito.when(experienceRepository.findByCandidatId(1L)).thenReturn(List.of(experience));

        BigDecimal score = matchingService.calculerScore(candidat, offre);

        // 60 (diplôme) + 12/24 * 40 = 60 + 20 = 80
        assertEquals(new BigDecimal("80.00"), score);
        assertTrue(matchingService.estAccepteAutomatiquement(score, offre));
    }
}
