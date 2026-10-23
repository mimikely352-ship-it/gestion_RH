package com.sirh.evaluation.service;
import com.sirh.commun.enums.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.evaluation.entity.*;
import com.sirh.evaluation.repository.*;
import com.sirh.recrutement.entity.*;
import com.sirh.recrutement.service.CandidatureService;
import com.sirh.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ParcoursEvaluationTest {
    final PassageTestRepository passages=mock(PassageTestRepository.class);
    final ReponseTestRepository reponses=mock(ReponseTestRepository.class);
    final ChoixTestRepository choix=mock(ChoixTestRepository.class);
    final TestRepository testsRepo=mock(TestRepository.class);
    final TestService tests=mock(TestService.class);
    final CandidatureService candidatures=mock(CandidatureService.class);
    final NotificationService notifications=mock(NotificationService.class);
    final PassageTestService sut=new PassageTestService(passages,reponses,choix,testsRepo,tests,candidatures,notifications);
    void preparer() {
        var offre=OffreEmploi.builder().id(1L).build();
        when(candidatures.obtenir(2L)).thenReturn(Candidature.builder().offre(offre).statut(StatutCandidature.EN_TEST).build());
        when(tests.obtenir(3L)).thenReturn(com.sirh.evaluation.entity.Test.builder().id(3L).offre(offre).niveau(1).build());
        when(tests.questionsDe(3L)).thenReturn(List.of(QuestionTest.builder().id(4L).build()));
        when(passages.save(any())).thenAnswer(i->i.getArgument(0));
    }
    @Test void refuseChoixDuneAutreQuestion() {
        preparer();
        when(choix.findById(5L)).thenReturn(Optional.of(ChoixTest.builder().id(5L).estCorrect(true)
                .question(QuestionTest.builder().id(999L).build()).build()));
        assertThrows(BusinessException.class,()->sut.soumettre(2L,3L,Map.of(4L,5L)));
        verify(reponses,never()).save(any());
    }
    @Test void refuseTestDuneAutreOffre() {
        preparer(); when(tests.obtenir(3L)).thenReturn(com.sirh.evaluation.entity.Test.builder()
                .offre(OffreEmploi.builder().id(99L).build()).build());
        assertThrows(BusinessException.class,()->sut.soumettre(2L,3L,Map.of()));
        verify(passages,never()).save(any());
    }
    @Test void refuseSecondeSoumission() {
        preparer(); when(passages.existsByCandidatureIdAndTestId(2L,3L)).thenReturn(true);
        assertThrows(BusinessException.class,()->sut.soumettre(2L,3L,Map.of()));
        verify(passages,never()).save(any());
    }
    @Test void refuseTestAvantEtapeTests() {
        preparer(); when(candidatures.obtenir(2L)).thenReturn(Candidature.builder().statut(StatutCandidature.RECUE).build());
        assertThrows(BusinessException.class,()->sut.soumettre(2L,3L,Map.of()));
    }
    @Test void refuseReponseApresEvaluationSoumise() {
        var repo=mock(EvaluationRepository.class);
        var service=new EvaluationService(repo,mock(QuestionEvaluationRepository.class),mock(ChoixEvaluationRepository.class),
                mock(ReponseEvaluationRepository.class),candidatures,notifications);
        when(repo.findById(1L)).thenReturn(Optional.of(Evaluation.builder().decisionRh(Decision.EN_ATTENTE)
                .dateEvaluation(LocalDateTime.now()).build()));
        assertThrows(BusinessException.class,()->service.soumettre(1L,Map.of(),Map.of()));
    }
}
