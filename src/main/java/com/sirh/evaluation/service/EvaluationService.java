package com.sirh.evaluation.service;

import com.sirh.commun.enums.Decision;
import com.sirh.commun.enums.FormatEvaluation;
import com.sirh.commun.enums.StatutCandidature;
import com.sirh.commun.enums.TypeNotification;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.evaluation.entity.ChoixEvaluation;
import com.sirh.evaluation.entity.Evaluation;
import com.sirh.evaluation.entity.QuestionEvaluation;
import com.sirh.evaluation.entity.ReponseEvaluation;
import com.sirh.evaluation.repository.ChoixEvaluationRepository;
import com.sirh.evaluation.repository.EvaluationRepository;
import com.sirh.evaluation.repository.QuestionEvaluationRepository;
import com.sirh.evaluation.repository.ReponseEvaluationRepository;
import com.sirh.notification.service.NotificationService;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.service.CandidatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Évaluation finale : format QCM ou texte libre au choix du RH ; décision RH, pas de seuil automatique. */
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final QuestionEvaluationRepository questionEvaluationRepository;
    private final ChoixEvaluationRepository choixEvaluationRepository;
    private final ReponseEvaluationRepository reponseEvaluationRepository;
    private final CandidatureService candidatureService;
    private final NotificationService notificationService;

    public record ChoixSaisi(String libelle, boolean estCorrect) {
    }

    public record QuestionSaisie(String enonce, List<ChoixSaisi> choix) {
    }

    @Transactional
    public Evaluation creer(Long candidatureId, String titre, FormatEvaluation format, List<QuestionSaisie> questions) {
        Candidature candidature = candidatureService.obtenir(candidatureId);
        Evaluation evaluation = Evaluation.builder()
                .candidature(candidature)
                .titre(titre)
                .format(format)
                .decisionRh(Decision.EN_ATTENTE)
                .build();
        evaluation = evaluationRepository.save(evaluation);

        notificationService.notifier(candidature.getCandidat().getUtilisateur().getId(),
                "Une évaluation (\"" + titre + "\") vous attend pour la candidature \"" + candidature.getOffre().getTitre() + "\".",
                com.sirh.commun.enums.TypeNotification.EVALUATION, "/candidat/evaluations/" + evaluation.getId() + "/repondre");

        if (questions != null) {
            int ordre = 0;
            for (QuestionSaisie q : questions) {
                QuestionEvaluation question = QuestionEvaluation.builder()
                        .evaluation(evaluation)
                        .enonce(q.enonce())
                        .type(format)
                        .ordre(ordre++)
                        .build();
                question = questionEvaluationRepository.save(question);
                if (format == FormatEvaluation.QCM && q.choix() != null) {
                    for (ChoixSaisi c : q.choix()) {
                        choixEvaluationRepository.save(ChoixEvaluation.builder()
                                .question(question)
                                .libelle(c.libelle())
                                .estCorrect(c.estCorrect())
                                .build());
                    }
                }
            }
        }
        return evaluation;
    }

    @Transactional
    public QuestionEvaluation ajouterQuestion(Long evaluationId, String enonce, List<ChoixSaisi> choix) {
        Evaluation evaluation = obtenir(evaluationId);
        int ordre = questionEvaluationRepository.findByEvaluationIdOrderByOrdreAsc(evaluationId).size();
        QuestionEvaluation question = QuestionEvaluation.builder()
                .evaluation(evaluation)
                .enonce(enonce)
                .type(evaluation.getFormat())
                .ordre(ordre)
                .build();
        question = questionEvaluationRepository.save(question);
        if (evaluation.getFormat() == FormatEvaluation.QCM && choix != null) {
            for (ChoixSaisi c : choix) {
                if (c.libelle() == null || c.libelle().isBlank()) {
                    continue;
                }
                choixEvaluationRepository.save(ChoixEvaluation.builder()
                        .question(question)
                        .libelle(c.libelle())
                        .estCorrect(c.estCorrect())
                        .build());
            }
        }
        return question;
    }

    /** @param reponsesQcm clé = questionId, valeur = choixId (ignoré si format TEXTE) */
    @Transactional
    public void soumettre(Long evaluationId, Map<Long, Long> reponsesQcm, Map<Long, String> reponsesTexte) {
        Evaluation evaluation = obtenir(evaluationId);
        List<QuestionEvaluation> questions = questionEvaluationRepository.findByEvaluationIdOrderByOrdreAsc(evaluationId);
        for (QuestionEvaluation question : questions) {
            ReponseEvaluation.ReponseEvaluationBuilder builder = ReponseEvaluation.builder()
                    .evaluation(evaluation)
                    .question(question);
            if (evaluation.getFormat() == FormatEvaluation.QCM) {
                Long choixId = reponsesQcm == null ? null : reponsesQcm.get(question.getId());
                if (choixId != null) {
                    ChoixEvaluation choix = choixEvaluationRepository.findById(choixId)
                            .orElseThrow(() -> new ResourceNotFoundException("Choix introuvable : " + choixId));
                    builder.choix(choix);
                }
            } else {
                String texte = reponsesTexte == null ? null : reponsesTexte.get(question.getId());
                builder.texteLibre(texte);
            }
            reponseEvaluationRepository.save(builder.build());
        }
        evaluation.setDateEvaluation(LocalDateTime.now());
        evaluationRepository.save(evaluation);
    }

    @Transactional
    public Evaluation decider(Long evaluationId, Decision decision) {
        Evaluation evaluation = obtenir(evaluationId);
        evaluation.setDecisionRh(decision);
        evaluation = evaluationRepository.save(evaluation);

        Candidature candidature = evaluation.getCandidature();
        StatutCandidature nouveauStatut = decision == Decision.ACCEPTE
                ? StatutCandidature.RETENUE
                : StatutCandidature.REFUSEE;
        candidatureService.changerStatut(candidature.getId(), nouveauStatut);

        String message = decision == Decision.ACCEPTE
                ? "Vous êtes retenu pour le poste \"" + candidature.getOffre().getTitre() + "\" !"
                : "Votre candidature pour \"" + candidature.getOffre().getTitre() + "\" n'est pas retenue après évaluation.";
        notificationService.notifier(candidature.getCandidat().getUtilisateur().getId(), message,
                TypeNotification.EVALUATION, "/candidat/mes-candidatures");
        return evaluation;
    }

    public Evaluation obtenir(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation introuvable : " + id));
    }

    public List<Evaluation> listerPourCandidature(Long candidatureId) {
        return evaluationRepository.findByCandidatureId(candidatureId);
    }

    public List<QuestionEvaluation> questionsDe(Long evaluationId) {
        return questionEvaluationRepository.findByEvaluationIdOrderByOrdreAsc(evaluationId);
    }

    public List<ReponseEvaluation> reponsesDe(Long evaluationId) {
        return reponseEvaluationRepository.findByEvaluationId(evaluationId);
    }
}
