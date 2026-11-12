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
import java.time.LocalDateTime;
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

    private Onboarding onboarding(StatutOnboarding statut, OnboardingTache... lignes) {
        var o = Onboarding.builder().id(7L).dossierId(1L).statut(statut).dateDebut(LocalDate.now())
                .termineLe(statut == StatutOnboarding.TERMINE ? LocalDateTime.now() : null).build();
        when(onboardings.findByDossierId(1L)).thenReturn(Optional.of(o));
        var liste = new ArrayList<OnboardingTache>();
        for (var t : lignes) { t.setOnboarding(o); liste.add(t); }
        when(taches.findByOnboardingId(7L)).thenReturn(liste);
        return o;
    }

    private static OnboardingTache fait(TacheOnboarding code, String par) {
        return OnboardingTache.builder().code(code).statut(StatutTache.FAIT).faitLe(LocalDateTime.now()).faitPar(par).build();
    }

    @Test void checklistConformeAuDocument() {
        assertEquals(7, TacheOnboarding.values().length);
        assertEquals("RH", TacheOnboarding.COMPTE_BANCAIRE.getResponsable());
        assertTrue(TacheOnboarding.MATERIEL.isManager());
        assertTrue(TacheOnboarding.FORMATION.isManager());
        assertFalse(TacheOnboarding.BADGE.isManager());
        assertEquals("Admin", TacheOnboarding.COMPTE.getResponsable());
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
        var o = onboarding(StatutOnboarding.EN_COURS);
        when(contrats.findFirstByDossierIdAndStatutNotOrderByDateDebutAscIdAsc(1L, StatutContrat.ANNULE)).thenReturn(Optional.of(
                Contrat.builder().type(TypeContrat.CDI).statut(StatutContrat.EN_COURS).dateSignature(LocalDate.now()).build()));

        sut.enregistrer(1L, EnumSet.of(TacheOnboarding.BADGE, TacheOnboarding.MATERIEL, TacheOnboarding.FORMATION,
                TacheOnboarding.INSCRIPTION_PAIE, TacheOnboarding.COMPTE_BANCAIRE), "rh@sirh.local");

        assertEquals(StatutOnboarding.TERMINE, o.getStatut());
        assertNotNull(o.getTermineLe());
        verify(taches, times(TacheOnboarding.values().length)).save(any());
    }

    @Test void decocherUneTacheRouvreLeWorkflow() {
        when(dossiers.verrouillerParId(1L)).thenReturn(Optional.of(dossier));
        var badge = fait(TacheOnboarding.BADGE, "rh");
        var o = onboarding(StatutOnboarding.TERMINE, badge);

        sut.enregistrer(1L, Set.of(), "rh@sirh.local");

        assertEquals(StatutTache.A_FAIRE, badge.getStatut());
        assertNull(badge.getFaitLe());
        assertEquals(StatutOnboarding.EN_COURS, o.getStatut());
        assertNull(o.getTermineLe());
    }

    @Test void unRenouvellementNeRouvrePasUneIntegrationTerminee() {
        when(dossiers.findById(1L)).thenReturn(Optional.of(dossier));
        var o = onboarding(StatutOnboarding.TERMINE, fait(TacheOnboarding.BADGE, "rh"));

        sut.synchroniser(1L); // appelé après l'enregistrement d'un nouveau contrat, non signé

        assertEquals(StatutOnboarding.TERMINE, o.getStatut());
        verify(taches, never()).save(any());
    }

    @Test void leChefDeServiceNeModifieQueSesTaches() {
        when(dossiers.verrouillerParId(1L)).thenReturn(Optional.of(dossier));
        var badge = fait(TacheOnboarding.BADGE, "rh@sirh.local");
        onboarding(StatutOnboarding.EN_COURS, badge);
        var enregistrees = new EnumMap<TacheOnboarding, OnboardingTache>(TacheOnboarding.class);
        when(taches.save(any())).thenAnswer(i -> { OnboardingTache t = i.getArgument(0); enregistrees.put(t.getCode(), t); return t; });

        // BADGE absent de la sélection et INSCRIPTION_PAIE cochée : ni l'une ni l'autre ne relève du responsable.
        sut.enregistrerParManager(1L, EnumSet.of(TacheOnboarding.MATERIEL, TacheOnboarding.INSCRIPTION_PAIE), "chef@sirh.local");

        assertEquals(StatutTache.FAIT, enregistrees.get(TacheOnboarding.MATERIEL).getStatut());
        assertEquals("chef@sirh.local", enregistrees.get(TacheOnboarding.MATERIEL).getFaitPar());
        assertEquals(StatutTache.A_FAIRE, enregistrees.get(TacheOnboarding.FORMATION).getStatut());
        assertEquals(StatutTache.FAIT, badge.getStatut(), "tâche RH conservée");
        assertEquals("rh@sirh.local", badge.getFaitPar());
        assertEquals(StatutTache.A_FAIRE, enregistrees.get(TacheOnboarding.INSCRIPTION_PAIE).getStatut());
    }

    @Test void integrationTermineeGardeSonEtatMemeApresUnDepart() {
        dossier.getUtilisateur().setActif(false); // compte désactivé au départ
        var o = Onboarding.builder().id(7L).dossierId(1L).statut(StatutOnboarding.TERMINE).termineLe(LocalDateTime.now()).build();
        var lignes = Arrays.stream(TacheOnboarding.values()).map(t -> fait(t, "rh")).toList();
        var vue = OnboardingService.calculer(dossier, null, o, lignes);
        assertTrue(vue.isTermine());
        assertEquals(7, vue.getFaits());
        assertTrue(parCode(vue).get(TacheOnboarding.COMPTE).fait());
    }
}
