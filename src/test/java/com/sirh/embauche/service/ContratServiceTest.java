package com.sirh.embauche.service;

import com.sirh.commun.enums.ActionAudit;
import com.sirh.commun.enums.StatutContrat;
import com.sirh.commun.enums.TypeContrat;
import com.sirh.commun.exception.BusinessException;
import com.sirh.embauche.entity.Contrat;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.embauche.entity.Remuneration;
import com.sirh.embauche.repository.ContratRepository;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContratServiceTest {
    final ContratRepository contrats = mock(ContratRepository.class);
    final DossierEmployeRepository dossiers = mock(DossierEmployeRepository.class);
    final OnboardingService onboarding = mock(OnboardingService.class);
    final com.sirh.embauche.repository.RemunerationRepository remunerations = mock(com.sirh.embauche.repository.RemunerationRepository.class);
    final AuditDossierService audit = mock(AuditDossierService.class);
    final ContratService sut = new ContratService(contrats, dossiers, mock(DocumentEmployeService.class), onboarding, remunerations, audit);
    final DossierEmploye dossier = DossierEmploye.builder().id(1L)
            .utilisateur(Utilisateur.builder().id(2L).email("e@test.local").actif(true).build()).build();
    final LocalDate aujourdhui = LocalDate.now();

    @BeforeEach void init() {
        when(contrats.dossierId(anyLong())).thenReturn(Optional.of(1L));
        when(dossiers.verrouillerParId(1L)).thenReturn(Optional.of(dossier));
        when(contrats.save(any())).thenAnswer(i -> {
            Contrat c = i.getArgument(0);
            if (c.getId() == null) c.setId(99L);
            return c;
        });
        when(contrats.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Contrat enCours(TypeContrat type, LocalDate debut, LocalDate finPrevue) {
        var c = Contrat.builder().id(5L).dossierId(1L).type(type).statut(StatutContrat.EN_COURS)
                .dateDebut(debut).dateFinPrevue(finPrevue).build();
        when(contrats.findFirstByDossierIdAndStatut(1L, StatutContrat.EN_COURS)).thenReturn(Optional.of(c));
        return c;
    }

    @Test void passageEnCdiTermineLeCddSansLEffacerEtLeReference() {
        var cdd = enCours(TypeContrat.CDD, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

        var cdi = sut.ajouter(1L, TypeContrat.CDI, LocalDate.of(2026, 7, 1), LocalDate.of(2027, 1, 1), null, null, "rh@sirh.local");

        assertEquals(StatutContrat.TERMINE, cdd.getStatut());
        assertEquals(LocalDate.of(2026, 6, 30), cdd.getDateFinEffective());
        verify(contrats).saveAndFlush(cdd);
        assertEquals(StatutContrat.EN_COURS, cdi.getStatut());
        assertEquals(5L, cdi.getContratPrecedentId(), "le renouvellement référence le contrat remplacé");
        assertNull(cdi.getDateFinPrevue(), "un CDI n'a pas de date de fin");
        verify(onboarding).synchroniser(1L);
    }

    @Test void renouvellementAnticipeResteAVenirEtLeContratActuelContinue() {
        var actuel = enCours(TypeContrat.CDD, aujourdhui.minusMonths(1), aujourdhui.plusMonths(2));

        var suivant = sut.ajouter(1L, TypeContrat.CDD, aujourdhui.plusMonths(2).plusDays(1), aujourdhui.plusMonths(8),
                new BigDecimal("1600000"), null, "rh@sirh.local");

        assertEquals(StatutContrat.A_VENIR, suivant.getStatut());
        assertEquals(5L, suivant.getContratPrecedentId());
        assertEquals(StatutContrat.EN_COURS, actuel.getStatut(), "le contrat actuel reste applicable jusqu'à la veille");
        assertNull(actuel.getDateFinEffective());
        verify(contrats, never()).saveAndFlush(actuel);
        verify(remunerations).save(argThat(r -> r.getDateEffet().equals(aujourdhui.plusMonths(2).plusDays(1))));
    }

    @Test void unSeulContratAVenirALaFois() {
        when(contrats.findFirstByDossierIdAndStatut(1L, StatutContrat.A_VENIR)).thenReturn(Optional.of(Contrat.builder()
                .id(6L).dossierId(1L).type(TypeContrat.CDI).statut(StatutContrat.A_VENIR).dateDebut(aujourdhui.plusDays(20)).build()));
        var erreur = assertThrows(BusinessException.class,
                () -> sut.ajouter(1L, TypeContrat.CDI, aujourdhui.plusDays(40), null, null, null, "rh"));
        assertTrue(erreur.getMessage().contains("annulez-le"));
    }

    @Test void entreeEnVigueurTermineLeContratEnCoursEtJournalise() {
        var actuel = enCours(TypeContrat.CDD, aujourdhui.minusMonths(6), aujourdhui.minusDays(1));
        var suivant = Contrat.builder().id(6L).dossierId(1L).type(TypeContrat.CDI).statut(StatutContrat.A_VENIR)
                .dateDebut(aujourdhui).contratPrecedentId(5L).build();
        when(contrats.findById(6L)).thenReturn(Optional.of(suivant));

        assertTrue(sut.activer(6L));

        assertEquals(StatutContrat.EN_COURS, suivant.getStatut());
        assertEquals(StatutContrat.TERMINE, actuel.getStatut());
        assertEquals(aujourdhui.minusDays(1), actuel.getDateFinEffective());
        verify(audit).journaliserDansTransaction(eq(1L), eq("système"), isNull(), eq(ActionAudit.CONTRAT_ACTIVE), contains("CDI"));
        verify(onboarding).synchroniser(1L);
    }

    @Test void contratFuturNonActive() {
        var futur = Contrat.builder().id(6L).dossierId(1L).type(TypeContrat.CDI).statut(StatutContrat.A_VENIR)
                .dateDebut(aujourdhui.plusDays(1)).build();
        when(contrats.findById(6L)).thenReturn(Optional.of(futur));
        assertFalse(sut.activer(6L));
        assertEquals(StatutContrat.A_VENIR, futur.getStatut());
    }
    @Test void activationVerrouilleLeDossierAvantDeLireLeContrat() {
        var annule = Contrat.builder().id(6L).dossierId(1L).statut(StatutContrat.ANNULE).dateDebut(aujourdhui).build();
        when(contrats.findById(6L)).thenReturn(Optional.of(annule));
        assertFalse(sut.activer(6L));
        var ordre = inOrder(contrats,dossiers);
        ordre.verify(contrats).dossierId(6L);
        ordre.verify(dossiers).verrouillerParId(1L);
        ordre.verify(contrats).findById(6L);
        verify(contrats,never()).save(any());
    }
    @Test void activationIgnoreUnSalarieParti() {
        dossier.getUtilisateur().setActif(false);
        assertFalse(sut.activer(6L));
        verify(contrats,never()).findById(anyLong());
        verify(contrats,never()).save(any());
    }

    @Test void annulationDUnContratAVenirRetireSesSalairesJamaisAppliques() {
        var prevu = Contrat.builder().id(6L).dossierId(1L).type(TypeContrat.CDD).statut(StatutContrat.A_VENIR)
                .dateDebut(aujourdhui.plusDays(30)).build();
        when(contrats.findById(6L)).thenReturn(Optional.of(prevu));
        var ligne = Remuneration.builder().id(3L).contratId(6L).dateEffet(aujourdhui.plusDays(30)).build();
        when(remunerations.findByContratIdAndDateEffetAfter(6L, aujourdhui)).thenReturn(List.of(ligne));

        sut.annulerAVenir(1L, 6L, "rh@sirh.local");

        assertEquals(StatutContrat.ANNULE, prevu.getStatut());
        assertTrue(prevu.getMotifFin().contains("rh@sirh.local"));
        verify(remunerations).deleteAll(List.of(ligne));
    }

    @Test void contratEnCoursNonAnnulable() {
        var actuel = enCours(TypeContrat.CDI, aujourdhui.minusYears(1), null);
        when(contrats.findById(5L)).thenReturn(Optional.of(actuel));
        assertThrows(BusinessException.class, () -> sut.annulerAVenir(1L, 5L, "rh"));
        assertEquals(StatutContrat.EN_COURS, actuel.getStatut());
    }

    @Test void cddSansDateDeFinRefuse() {
        assertThrows(BusinessException.class,
                () -> sut.ajouter(1L, TypeContrat.CDD, LocalDate.of(2026, 1, 1), null, null, null, "rh"));
        verify(contrats, never()).save(any());
    }

    @Test void nouveauContratAvantLeContratEnCoursRefuse() {
        var actuel = enCours(TypeContrat.CDI, LocalDate.of(2026, 3, 1), null);
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
