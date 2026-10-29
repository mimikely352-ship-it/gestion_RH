package com.sirh.evaluation.service;

import com.sirh.commun.enums.DecisionTest;
import com.sirh.commun.enums.StatutCandidature;
import com.sirh.commun.enums.TypeNotification;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.evaluation.entity.ChoixTest;
import com.sirh.evaluation.entity.PassageTest;
import com.sirh.evaluation.entity.QuestionTest;
import com.sirh.evaluation.entity.ReponseTest;
import com.sirh.evaluation.repository.ChoixTestRepository;
import com.sirh.evaluation.repository.PassageTestRepository;
import com.sirh.evaluation.repository.ReponseTestRepository;
import com.sirh.evaluation.repository.TestRepository;
import com.sirh.notification.service.NotificationService;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.service.CandidatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Passage d'un test par un candidat : note calculée automatiquement, décision laissée au RH. */
@Service
@RequiredArgsConstructor
public class PassageTestService {

    private final PassageTestRepository passageTestRepository;
    private final ReponseTestRepository reponseTestRepository;
    private final ChoixTestRepository choixTestRepository;
    private final TestRepository testRepository;
    private final TestService testService;
    private final CandidatureService candidatureService;
    private final NotificationService notificationService;

    /** @param reponses clé = questionId, valeur = choixId sélectionné */
    @Transactional
    public PassageTest soumettre(Long candidatureId, Long testId, Map<Long, Long> reponses) {
        Candidature candidature = candidatureService.obtenir(candidatureId);
        var test = testService.obtenir(testId);
        if (candidature.getStatut() != StatutCandidature.EN_TEST || !test.getOffre().getId().equals(candidature.getOffre().getId()))
            throw new com.sirh.commun.exception.BusinessException("Ce test n’est pas accessible pour cette candidature à cette étape.");
        if (passageTestRepository.existsByCandidatureIdAndTestId(candidatureId, testId))
            throw new com.sirh.commun.exception.BusinessException("Ce test a déjà été soumis.");
        var anterieurs = testRepository.findByOffreIdOrderByNiveauAsc(candidature.getOffre().getId());
        var dejaPasses = passageTestRepository.findByCandidatureId(candidatureId);
        if (anterieurs.stream().filter(t -> t.getNiveau() < test.getNiveau()).anyMatch(t -> dejaPasses.stream()
                .noneMatch(p -> p.getTest().getId().equals(t.getId()) && p.getDecisionRh() == DecisionTest.REUSSI)))
            throw new com.sirh.commun.exception.BusinessException("Les niveaux précédents doivent être validés par les RH.");
        List<QuestionTest> questions = testService.questionsDe(testId);
        if (questions.isEmpty()) throw new com.sirh.commun.exception.BusinessException("Ce test ne contient aucune question.");

        PassageTest passage = PassageTest.builder()
                .candidature(candidature)
                .test(test)
                .datePassage(LocalDateTime.now())
                .decisionRh(DecisionTest.EN_ATTENTE)
                .build();
        passage = passageTestRepository.save(passage);

        int bonnes = 0;
        for (QuestionTest question : questions) {
            Long choixId = reponses.get(question.getId());
            ChoixTest choix = choixId == null ? null : choixTestRepository.findById(choixId)
                    .orElseThrow(() -> new ResourceNotFoundException("Choix introuvable : " + choixId));
            if (choix != null && !choix.getQuestion().getId().equals(question.getId()))
                throw new com.sirh.commun.exception.BusinessException("Une réponse ne correspond pas à sa question.");
            reponseTestRepository.save(ReponseTest.builder()
                    .passage(passage)
                    .question(question)
                    .choix(choix)
                    .build());
            if (choix != null && choix.isEstCorrect()) {
                bonnes++;
            }
        }
        BigDecimal note = questions.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(bonnes).multiply(BigDecimal.valueOf(20))
                    .divide(BigDecimal.valueOf(questions.size()), 2, RoundingMode.HALF_UP);
        passage.setNote(note);
        return passageTestRepository.save(passage);
    }

    @Transactional
    public PassageTest decider(Long passageId, DecisionTest decision) {
        PassageTest passage = passageTestRepository.findById(passageId)
                .orElseThrow(() -> new ResourceNotFoundException("Passage de test introuvable : " + passageId));
        if (decision == null || decision == DecisionTest.EN_ATTENTE || passage.getDecisionRh() != DecisionTest.EN_ATTENTE
                || passage.getCandidature().getStatut() != StatutCandidature.EN_TEST)
            throw new com.sirh.commun.exception.BusinessException("Ce test ne peut plus recevoir cette décision.");
        passage.setDecisionRh(decision);
        passage = passageTestRepository.save(passage);

        Candidature candidature = passage.getCandidature();
        Long utilisateurId = candidature.getCandidat().getUtilisateur().getId();

        if (decision == DecisionTest.ECHEC) {
            candidatureService.changerStatut(candidature.getId(), StatutCandidature.REFUSEE);
            notificationService.notifier(utilisateurId,
                    "Votre résultat au test \"" + passage.getTest().getTitre() + "\" ne permet pas de poursuivre.",
                    TypeNotification.TEST, "/candidat/mes-candidatures");
        } else if (decision == DecisionTest.REUSSI) {
            notificationService.notifier(utilisateurId,
                    "Vous avez réussi le test \"" + passage.getTest().getTitre() + "\".",
                    TypeNotification.TEST, "/candidat/mes-candidatures");
            if (tousLesNiveauxReussis(candidature)) {
                candidatureService.changerStatut(candidature.getId(), StatutCandidature.EN_EVALUATION);
            }
        }
        return passage;
    }

    private boolean tousLesNiveauxReussis(Candidature candidature) {
        var tousLesTests = testRepository.findByOffreIdOrderByNiveauAsc(candidature.getOffre().getId());
        if (tousLesTests.isEmpty()) {
            return false;
        }
        var passages = passageTestRepository.findByCandidatureId(candidature.getId());
        return tousLesTests.stream().allMatch(test -> passages.stream()
                .anyMatch(p -> p.getTest().getId().equals(test.getId()) && p.getDecisionRh() == DecisionTest.REUSSI));
    }

    public List<PassageTest> listerPourCandidature(Long candidatureId) {
        return passageTestRepository.findByCandidatureId(candidatureId);
    }

    /** Résultats de tous les candidats pour un test donné, à l'usage du RH. */
    public List<PassageTest> listerPourTest(Long testId) {
        return passageTestRepository.findByTestIdOrderByDatePassageDesc(testId);
    }

    public List<ReponseTest> reponsesDe(Long passageId) {
        return reponseTestRepository.findByPassageId(passageId);
    }
}
