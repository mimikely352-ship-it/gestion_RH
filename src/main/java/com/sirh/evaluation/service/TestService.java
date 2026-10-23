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
    private final com.sirh.evaluation.repository.PassageTestRepository passages;

    public record ChoixSaisi(String libelle, boolean estCorrect) {
    }

    public record QuestionSaisie(String enonce, List<ChoixSaisi> choix) {
    }

    @Transactional
    public Test creer(Long offreId, int niveau, String titre, List<QuestionSaisie> questions) {
        if (titre == null || titre.isBlank() || titre.length() > 150) throw new BusinessException("Renseignez un titre de test de 150 caractères maximum.");
        if (questions != null) questions.forEach(q -> verifierQuestion(q.enonce(), q.choix()));
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
        verifierQuestion(enonce, choix);
        if (passages.existsByTestId(testId)) throw new BusinessException("Un test déjà passé ne peut plus être modifié.");
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

    private void verifierQuestion(String enonce, List<ChoixSaisi> choix) {
        if (enonce == null || enonce.isBlank() || choix == null) throw new BusinessException("Précisez la question et ses choix.");
        var renseignes = choix.stream().filter(c -> c.libelle() != null && !c.libelle().isBlank()).toList();
        if (renseignes.size() < 2 || renseignes.stream().anyMatch(c -> c.libelle().length() > 255)
                || renseignes.stream().filter(ChoixSaisi::estCorrect).count() != 1)
            throw new BusinessException("Une question requiert au moins deux choix et une seule bonne réponse.");
    }
}
