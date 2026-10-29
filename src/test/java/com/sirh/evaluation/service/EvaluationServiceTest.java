package com.sirh.evaluation.service;

import com.sirh.commun.enums.Decision;
import com.sirh.commun.enums.FormatEvaluation;
import com.sirh.commun.enums.StatutCandidature;
import com.sirh.evaluation.entity.*;
import com.sirh.evaluation.repository.*;
import com.sirh.notification.service.NotificationService;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.service.CandidatureService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Une évaluation peut mélanger des questions QCM et texte libre, indépendamment du format déclaré à la création. */
class EvaluationServiceTest {
    final EvaluationRepository evaluations = mock(EvaluationRepository.class);
    final QuestionEvaluationRepository questions = mock(QuestionEvaluationRepository.class);
    final ChoixEvaluationRepository choix = mock(ChoixEvaluationRepository.class);
    final ReponseEvaluationRepository reponses = mock(ReponseEvaluationRepository.class);
    final CandidatureService candidatures = mock(CandidatureService.class);
    final NotificationService notifications = mock(NotificationService.class);
    final EvaluationService sut = new EvaluationService(evaluations, questions, choix, reponses, candidatures, notifications);

    final Candidature candidature = Candidature.builder().id(1L).statut(StatutCandidature.EN_EVALUATION).build();
    final Evaluation evaluation = Evaluation.builder().id(10L).candidature(candidature)
            .format(FormatEvaluation.QCM).decisionRh(Decision.EN_ATTENTE).build();

    void preparer() {
        when(evaluations.findById(10L)).thenReturn(Optional.of(evaluation));
        when(questions.findByEvaluationIdOrderByOrdreAsc(10L)).thenReturn(List.of());
        when(questions.save(any())).thenAnswer(i -> i.getArgument(0));
        when(choix.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void ajouteUneQuestionTexteLibreDansUneEvaluationQcm() {
        preparer();
        var question = sut.ajouterQuestion(10L, "Décrivez votre expérience", FormatEvaluation.TEXTE, null);
        assertEquals(FormatEvaluation.TEXTE, question.getType());
        verify(choix, never()).save(any());
    }

    @Test void ajouteUneQuestionQcmAvecSesChoix() {
        preparer();
        var choixSaisis = List.of(new EvaluationService.ChoixSaisi("Oui", true), new EvaluationService.ChoixSaisi("Non", false));
        var question = sut.ajouterQuestion(10L, "Êtes-vous disponible ?", FormatEvaluation.QCM, choixSaisis);
        assertEquals(FormatEvaluation.QCM, question.getType());
        verify(choix, times(2)).save(any());
    }

    @Test void soumissionEnregistreChoixPourQuestionQcmEtTextePourQuestionTexte() {
        preparer();
        var questionQcm = QuestionEvaluation.builder().id(20L).evaluation(evaluation).type(FormatEvaluation.QCM).build();
        var questionTexte = QuestionEvaluation.builder().id(21L).evaluation(evaluation).type(FormatEvaluation.TEXTE).build();
        when(questions.findByEvaluationIdOrderByOrdreAsc(10L)).thenReturn(List.of(questionQcm, questionTexte));
        var choixCorrect = ChoixEvaluation.builder().id(30L).question(questionQcm).estCorrect(true).build();
        when(choix.findById(30L)).thenReturn(Optional.of(choixCorrect));
        when(reponses.save(any())).thenAnswer(i -> i.getArgument(0));

        sut.soumettre(10L, Map.of(20L, 30L), Map.of(21L, "Ma réponse libre"));

        verify(reponses).save(argThat(r -> r.getQuestion().getId().equals(20L) && r.getChoix() != null && r.getChoix().getId().equals(30L)));
        verify(reponses).save(argThat(r -> r.getQuestion().getId().equals(21L) && "Ma réponse libre".equals(r.getTexteLibre())));
    }
}
