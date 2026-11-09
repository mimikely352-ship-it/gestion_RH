package com.sirh.embauche.service;

import com.sirh.commun.enums.*;
import com.sirh.embauche.entity.Contrat;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.embauche.entity.Onboarding;
import com.sirh.embauche.entity.OnboardingTache;
import com.sirh.embauche.repository.ContratRepository;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.embauche.repository.OnboardingRepository;
import com.sirh.embauche.repository.OnboardingTacheRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnboardingServiceTest {
    final OnboardingRepository onboardings = mock(OnboardingRepository.class);
    final OnboardingTacheRepository taches = mock(OnboardingTacheRepository.class);
    final ContratRepository contrats = mock(ContratRepository.class);
    final DossierEmployeRepository dossiers = mock(DossierEmployeRepository.class);
    final OnboardingService sut = new OnboardingService(onboardings, taches, contrats, dossiers);

    final DossierEmploye dossier = DossierEmploye.builder().id(1L).dateEntree(LocalDate.of(2026, 1, 5))
            .utilisateur(Utilisateur.builder().id(2L).email("e@test.local").actif(true).build()).build();

    private static Map<TacheOnboarding, OnboardingService.Etape> parCode(OnboardingService.Vue v) {
        var m = new EnumMap<TacheOnboarding, OnboardingService.Etape>(TacheOnboarding.class);
        v.etapes().forEach(e -> m.put(e.tache(), e));
        return m;
    }

    @Test void compteCocheDesQueLeLoginExiste_contratSeulementSiSigne() {
        var nonSigne = Contrat.builder().type(TypeContrat.CDI).statut(StatutContrat.EN_COURS).build();
        var vue = OnboardingService.calculer(dossier, nonSigne, null, List.of());
        assertTrue(parCode(vue).get(TacheOnboarding.COMPTE).fait());
        assertFalse(parCode(vue).get(TacheOnboarding.CONTRAT_SIGNE).fait());
        assertEquals(StatutOnboarding.EN_COURS, vue.statut());

        nonSigne.setDateSignature(LocalDate.of(2026, 1, 6));
        assertTrue(parCode(OnboardingService.calculer(dossier, nonSigne, null, List.of())).get(TacheOnboarding.CONTRAT_SIGNE).fait());
    }

    @Test void workflowPasseATermineQuandToutEstFait() {
        when(dossiers.verrouillerParId(1L)).thenReturn(Optional.of(dossier));
        var onboarding = Onboarding.builder().id(7L).dossierId(1L).statut(StatutOnboarding.EN_COURS).dateDebut(LocalDate.now()).build();
        when(onboardings.findByDossierId(1L)).thenReturn(Optional.of(onboarding));
        when(taches.findByOnboardingId(7L)).thenReturn(List.of());
        when(contrats.findFirstByDossierIdAndStatut(1L, StatutContrat.EN_COURS)).thenReturn(Optional.of(
                Contrat.builder().type(TypeContrat.CDI).statut(StatutContrat.EN_COURS).dateSignature(LocalDate.now()).build()));

        sut.enregistrer(1L, EnumSet.of(TacheOnboarding.BADGE, TacheOnboarding.MATERIEL,
                TacheOnboarding.FORMATION, TacheOnboarding.INSCRIPTION_PAIE), "rh@sirh.local");

        assertEquals(StatutOnboarding.TERMINE, onboarding.getStatut());
        assertNotNull(onboarding.getTermineLe());
        verify(taches, times(TacheOnboarding.values().length)).save(any());
    }

    @Test void decocherUneTacheRouvreLeWorkflow() {
        when(dossiers.verrouillerParId(1L)).thenReturn(Optional.of(dossier));
        var onboarding = Onboarding.builder().id(7L).dossierId(1L).statut(StatutOnboarding.TERMINE).dateDebut(LocalDate.now())
                .termineLe(java.time.LocalDateTime.now()).build();
        when(onboardings.findByDossierId(1L)).thenReturn(Optional.of(onboarding));
        var badge = OnboardingTache.builder().onboarding(onboarding).code(TacheOnboarding.BADGE).statut(StatutTache.FAIT)
                .faitLe(java.time.LocalDateTime.now()).faitPar("rh").build();
        when(taches.findByOnboardingId(7L)).thenReturn(List.of(badge));

        sut.enregistrer(1L, Set.of(), "rh@sirh.local");

        assertEquals(StatutTache.A_FAIRE, badge.getStatut());
        assertNull(badge.getFaitLe());
        assertEquals(StatutOnboarding.EN_COURS, onboarding.getStatut());
        assertNull(onboarding.getTermineLe());
    }
}
