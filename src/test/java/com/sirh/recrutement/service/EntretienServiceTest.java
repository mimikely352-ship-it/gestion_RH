package com.sirh.recrutement.service;

import com.sirh.commun.enums.Decision;
import com.sirh.commun.enums.StatutCandidature;
import com.sirh.commun.enums.StatutEntretien;
import com.sirh.commun.exception.BusinessException;
import com.sirh.evaluation.repository.TestRepository;
import com.sirh.notification.service.NotificationService;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.entity.Entretien;
import com.sirh.recrutement.entity.OffreEmploi;
import com.sirh.recrutement.repository.EntretienRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EntretienServiceTest {
    private final EntretienRepository entretiens = mock(EntretienRepository.class);
    private final CandidatureService candidatures = mock(CandidatureService.class);
    private final TestRepository tests = mock(TestRepository.class);
    private final EntretienService service = new EntretienService(entretiens, candidatures, mock(NotificationService.class), tests);
    private final List<Entretien> enBase = new ArrayList<>();
    private Candidature candidature;

    @BeforeEach void preparer() {
        var utilisateur = com.sirh.utilisateur.entity.Utilisateur.builder().id(4L).build();
        candidature = Candidature.builder().id(1L).statut(StatutCandidature.ACCEPTEE_ENTRETIEN)
                .offre(OffreEmploi.builder().id(3L).titre("Comptable").build())
                .candidat(com.sirh.candidat.entity.Candidat.builder().utilisateur(utilisateur).build()).build();
        when(candidatures.obtenirPourModification(1L)).thenReturn(candidature);
        when(candidatures.changerStatut(eq(1L), any())).thenAnswer(i -> { candidature.setStatut(i.getArgument(1)); return candidature; });
        when(entretiens.findByCandidature_IdOrderByDateHeureAscIdAsc(1L)).thenAnswer(i -> List.copyOf(enBase));
        when(entretiens.save(any())).thenAnswer(i -> {
            Entretien e = i.getArgument(0);
            if (e.getId() == null) { e.setId((long) enBase.size() + 10); enBase.add(e); }
            return e;
        });
        when(entretiens.findById(any())).thenAnswer(i -> enBase.stream()
                .filter(e -> e.getId().equals(i.getArgument(0))).findFirst());
    }

    private Entretien planifierPuisPasser() {
        var e = service.planifier(1L, LocalDateTime.now().plusDays(enBase.size() + 1), "Salle A");
        e.setDateHeure(LocalDateTime.now().minusMinutes(5 - enBase.size())); // l'entretien a eu lieu
        return e;
    }

    @Test void plusieursEntretiensSuccessifsPuisTests() {
        when(tests.findByOffreIdOrderByNiveauAsc(3L)).thenReturn(List.of(new com.sirh.evaluation.entity.Test()));
        var premier = planifierPuisPasser();
        assertEquals(StatutCandidature.EN_ENTRETIEN, candidature.getStatut());
        assertFalse(service.peutPlanifier(candidature), "un entretien en attente bloque la planification");
        assertThrows(BusinessException.class, () -> service.planifier(1L, LocalDateTime.now().plusDays(9), null));

        service.decider(premier.getId(), EntretienService.Suite.ENTRETIEN_SUIVANT, "Bon profil");
        assertEquals(StatutEntretien.REALISE, premier.getStatut());
        assertEquals(Decision.ACCEPTE, premier.getDecisionRh());
        assertEquals(StatutCandidature.EN_ENTRETIEN, candidature.getStatut());
        assertTrue(service.peutPlanifier(candidature));

        var second = planifierPuisPasser();
        assertEquals(2, enBase.size());
        service.decider(second.getId(), EntretienService.Suite.TESTS, null);
        assertEquals(StatutCandidature.EN_TEST, candidature.getStatut());
        assertFalse(service.peutPlanifier(candidature));
        assertThrows(BusinessException.class, () -> service.decider(second.getId(), EntretienService.Suite.REFUS, null));
    }

    @Test void passageAuxTestsRefuseSansTestSurLOffre() {
        when(tests.findByOffreIdOrderByNiveauAsc(3L)).thenReturn(List.of());
        var e = planifierPuisPasser();
        assertThrows(BusinessException.class, () -> service.decider(e.getId(), EntretienService.Suite.TESTS, null));
        assertEquals(Decision.EN_ATTENTE, e.getDecisionRh());
        assertEquals(StatutCandidature.EN_ENTRETIEN, candidature.getStatut());
        service.decider(e.getId(), EntretienService.Suite.REFUS, "Profil éloigné");
        assertEquals(StatutCandidature.REFUSEE_ENTRETIEN, candidature.getStatut());
    }

    @Test void decisionImpossibleAvantLEntretien() {
        var e = service.planifier(1L, LocalDateTime.now().plusDays(1), null);
        assertThrows(BusinessException.class, () -> service.decider(e.getId(), EntretienService.Suite.REFUS, null));
        assertEquals(StatutEntretien.PLANIFIE, e.getStatut());
    }

    @Test void prochaineEtapeGuideLaRh() {
        assertEquals("Planifier le premier entretien.", service.prochaineEtape(candidature, true));
        var e = planifierPuisPasser();
        assertTrue(service.prochaineEtape(candidature, true).startsWith("Décider l’entretien n° 1"));
        service.decider(e.getId(), EntretienService.Suite.ENTRETIEN_SUIVANT, null);
        assertEquals("Planifier l’entretien n° 2.", service.prochaineEtape(candidature, true));
    }
}
