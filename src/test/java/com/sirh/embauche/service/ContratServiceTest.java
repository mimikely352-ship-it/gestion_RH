package com.sirh.embauche.service;

import com.sirh.commun.enums.StatutContrat;
import com.sirh.commun.enums.TypeContrat;
import com.sirh.commun.exception.BusinessException;
import com.sirh.embauche.entity.Contrat;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.embauche.repository.ContratRepository;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContratServiceTest {
    final ContratRepository contrats = mock(ContratRepository.class);
    final DossierEmployeRepository dossiers = mock(DossierEmployeRepository.class);
    final OnboardingService onboarding = mock(OnboardingService.class);
    final com.sirh.embauche.repository.RemunerationRepository remunerations = mock(com.sirh.embauche.repository.RemunerationRepository.class);
    final ContratService sut = new ContratService(contrats, dossiers, mock(DocumentEmployeService.class), onboarding, remunerations);
    final DossierEmploye dossier = DossierEmploye.builder().id(1L)
            .utilisateur(Utilisateur.builder().id(2L).email("e@test.local").actif(true).build()).build();

    @BeforeEach void init() {
        when(dossiers.verrouillerParId(1L)).thenReturn(Optional.of(dossier));
        when(contrats.save(any())).thenAnswer(i -> i.getArgument(0));
        when(contrats.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void passageEnCdiTermineLeCddSansLEffacer() {
        var cdd = Contrat.builder().id(5L).dossierId(1L).type(TypeContrat.CDD).statut(StatutContrat.EN_COURS)
                .dateDebut(LocalDate.of(2026, 1, 1)).dateFinPrevue(LocalDate.of(2026, 6, 30)).build();
        when(contrats.findFirstByDossierIdAndStatut(1L, StatutContrat.EN_COURS)).thenReturn(Optional.of(cdd));

        var cdi = sut.ajouter(1L, TypeContrat.CDI, LocalDate.of(2026, 7, 1), LocalDate.of(2027, 1, 1), null, null, "rh@sirh.local");

        assertEquals(StatutContrat.TERMINE, cdd.getStatut());
        assertEquals(LocalDate.of(2026, 6, 30), cdd.getDateFinEffective());
        verify(contrats).saveAndFlush(cdd);
        assertEquals(StatutContrat.EN_COURS, cdi.getStatut());
        assertNull(cdi.getDateFinPrevue(), "un CDI n'a pas de date de fin");
        verify(onboarding).synchroniser(1L);
    }

    @Test void cddSansDateDeFinRefuse() {
        assertThrows(BusinessException.class,
                () -> sut.ajouter(1L, TypeContrat.CDD, LocalDate.of(2026, 1, 1), null, null, null, "rh"));
        verify(contrats, never()).save(any());
    }

    @Test void nouveauContratAvantLeContratEnCoursRefuse() {
        var actuel = Contrat.builder().id(5L).dossierId(1L).type(TypeContrat.CDI).statut(StatutContrat.EN_COURS)
                .dateDebut(LocalDate.of(2026, 3, 1)).build();
        when(contrats.findFirstByDossierIdAndStatut(1L, StatutContrat.EN_COURS)).thenReturn(Optional.of(actuel));
        assertThrows(BusinessException.class,
                () -> sut.ajouter(1L, TypeContrat.CDI, LocalDate.of(2026, 2, 1), null, null, null, "rh"));
        assertEquals(StatutContrat.EN_COURS, actuel.getStatut());
    }

    @Test void employeParti() {
        dossier.getUtilisateur().setActif(false);
        assertThrows(BusinessException.class,
                () -> sut.ajouter(1L, TypeContrat.CDI, LocalDate.of(2026, 2, 1), null, null, null, "rh"));
    }
}
