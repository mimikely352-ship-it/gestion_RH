package com.sirh.evaluation.service;

import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.evaluation.entity.ChoixTest;
import com.sirh.evaluation.entity.QuestionTest;
import com.sirh.evaluation.entity.Test;
import com.sirh.evaluation.repository.ChoixTestRepository;
import com.sirh.evaluation.repository.QuestionTestRepository;
import com.sirh.evaluation.repository.TestRepository;
import com.sirh.recrutement.service.OffreEmploiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestion des tests QCM par niveau (1 à 5) créés par le RH pour une offre. */
@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final QuestionTestRepository questionTestRepository;
    private final ChoixTestRepository choixTestRepository;
    private final OffreEmploiService offreEmploiService;

    public record ChoixSaisi(String libelle, boolean estCorrect) {
    }

    public record QuestionSaisie(String enonce, List<ChoixSaisi> choix) {
    }

    @Transactional
    public Test creer(Long offreId, int niveau, String titre, List<QuestionSaisie> questions) {
        if (niveau < 1 || niveau > 5) {
            throw new BusinessException("Le niveau d'un test doit être compris entre 1 et 5.");
        }
        Test test = Test.builder()
                .offre(offreEmploiService.obtenir(offreId))
                .niveau(niveau)
                .titre(titre)
                .build();
        test = testRepository.save(test);

        if (questions != null) {
            int ordre = 0;
            for (QuestionSaisie q : questions) {
                QuestionTest question = QuestionTest.builder()
                        .test(test)
                        .enonce(q.enonce())
                        .ordre(ordre++)
                        .build();
                question = questionTestRepository.save(question);
                if (q.choix() != null) {
                    for (ChoixSaisi c : q.choix()) {
                        choixTestRepository.save(ChoixTest.builder()
                                .question(question)
                                .libelle(c.libelle())
                                .estCorrect(c.estCorrect())
                                .build());
                    }
                }
            }
        }
        return test;
    }

    @Transactional
    public QuestionTest ajouterQuestion(Long testId, String enonce, List<ChoixSaisi> choix) {
        Test test = obtenir(testId);
        int ordre = questionTestRepository.findByTestIdOrderByOrdreAsc(testId).size();
        QuestionTest question = QuestionTest.builder()
                .test(test)
                .enonce(enonce)
                .ordre(ordre)
                .build();
        question = questionTestRepository.save(question);
        if (choix != null) {
            for (ChoixSaisi c : choix) {
                if (c.libelle() == null || c.libelle().isBlank()) {
                    continue;
                }
                choixTestRepository.save(ChoixTest.builder()
                        .question(question)
                        .libelle(c.libelle())
                        .estCorrect(c.estCorrect())
                        .build());
            }
        }
        return question;
    }

    public List<Test> listerPourOffre(Long offreId) {
        return testRepository.findByOffreIdOrderByNiveauAsc(offreId);
    }

    public Test obtenir(Long id) {
        return testRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test introuvable : " + id));
    }

    public List<QuestionTest> questionsDe(Long testId) {
        return questionTestRepository.findByTestIdOrderByOrdreAsc(testId);
    }

    public List<ChoixTest> choixDe(Long questionId) {
        return choixTestRepository.findByQuestionId(questionId);
    }
}
