package com.sirh.recrutement.service;

import com.sirh.commun.enums.Decision;
import com.sirh.commun.enums.StatutCandidature;
import com.sirh.commun.enums.StatutEntretien;
import com.sirh.commun.enums.TypeNotification;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.evaluation.repository.TestRepository;
import com.sirh.notification.service.NotificationService;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.entity.Entretien;
import com.sirh.recrutement.repository.EntretienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entretiens successifs : une candidature peut passer plusieurs entretiens, un seul étant planifié à la fois.
 * Après chaque entretien réalisé, la RH choisit : entretien suivant, passage aux tests ou refus.
 */
@Service
@RequiredArgsConstructor
public class EntretienService {

    /** Suite donnée à un entretien réalisé. */
    public enum Suite { ENTRETIEN_SUIVANT, TESTS, REFUS }

    private final EntretienRepository entretienRepository;
    private final CandidatureService candidatureService;
    private final NotificationService notificationService;
    private final TestRepository testRepository;

    @Transactional
    public Entretien planifier(Long candidatureId, LocalDateTime dateHeure, String lieu) {
        if (dateHeure == null || !dateHeure.isAfter(LocalDateTime.now()))
            throw new BusinessException("Choisissez une date d’entretien future.");
        if (lieu != null && lieu.length() > 150)
            throw new BusinessException("Le lieu ne peut dépasser 150 caractères.");
        Candidature candidature = candidatureService.obtenirPourModification(candidatureId);
        if (!peutPlanifier(candidature))
            throw new BusinessException(candidature.getStatut() == StatutCandidature.EN_ENTRETIEN
                    ? "Un entretien est déjà planifié ou attend sa décision. Décidez-le avant d’en planifier un autre."
                    : "La candidature n'est pas au stade entretien.");
        var precedents = lister(candidatureId);
        if (!precedents.isEmpty() && !dateHeure.isAfter(precedents.get(precedents.size() - 1).getDateHeure()))
            throw new BusinessException("Le nouvel entretien doit avoir lieu après le précédent.");
        Entretien entretien = entretienRepository.save(Entretien.builder()
                .candidature(candidature)
                .dateHeure(dateHeure)
                .lieu(lieu)
                .statut(StatutEntretien.PLANIFIE)
                .decisionRh(Decision.EN_ATTENTE)
                .build());

        if (candidature.getStatut() == StatutCandidature.ACCEPTEE_ENTRETIEN)
            candidatureService.changerStatut(candidatureId, StatutCandidature.EN_ENTRETIEN);
        int numero = precedents.size() + 1;
        notificationService.notifier(candidature.getCandidat().getUtilisateur().getId(),
                (numero == 1 ? "Un entretien" : "L’entretien n° " + numero) + " est planifié pour votre candidature \""
                        + candidature.getOffre().getTitre() + "\".",
                TypeNotification.ENTRETIEN, "/candidat/mes-candidatures");
        return entretien;
    }

    /** Compatibilité : ACCEPTE envoie aux tests, REFUSE clôt la candidature. */
    @Transactional
    public Entretien decider(Long entretienId, Decision decision, String commentaire) {
        if (decision == null || decision == Decision.EN_ATTENTE)
            throw new BusinessException("Cet entretien ne peut plus recevoir cette décision.");
        return decider(entretienId, decision == Decision.ACCEPTE ? Suite.TESTS : Suite.REFUS, commentaire);
    }

    @Transactional
    public Entretien decider(Long entretienId, Suite suite, String commentaire) {
        Entretien entretien = entretienRepository.findById(entretienId)
                .orElseThrow(() -> new ResourceNotFoundException("Entretien introuvable : " + entretienId));
        Candidature candidature = candidatureService.obtenirPourModification(entretien.getCandidature().getId());
        if (suite == null || entretien.getStatut() != StatutEntretien.PLANIFIE
                || entretien.getDecisionRh() != Decision.EN_ATTENTE
                || candidature.getStatut() != StatutCandidature.EN_ENTRETIEN)
            throw new BusinessException("Cet entretien ne peut plus recevoir cette décision.");
        if (entretien.getDateHeure().isAfter(LocalDateTime.now()))
            throw new BusinessException("L’entretien n’a pas encore eu lieu.");
        if (commentaire != null && commentaire.length() > 5000)
            throw new BusinessException("Le commentaire ne peut dépasser 5 000 caractères.");
        if (suite == Suite.TESTS && testRepository.findByOffreIdOrderByNiveauAsc(candidature.getOffre().getId()).isEmpty())
            throw new BusinessException("Aucun test n’est défini pour cette offre. Créez au moins un test avant d’y envoyer le candidat.");

        entretien.setStatut(StatutEntretien.REALISE);
        entretien.setDecisionRh(suite == Suite.REFUS ? Decision.REFUSE : Decision.ACCEPTE);
        entretien.setCommentaire(commentaire);
        entretienRepository.save(entretien);

        String titre = candidature.getOffre().getTitre();
        String message = switch (suite) {
            case ENTRETIEN_SUIVANT -> "Vous êtes retenu pour un entretien supplémentaire pour \"" + titre + "\".";
            case TESTS -> {
                candidatureService.changerStatut(candidature.getId(), StatutCandidature.EN_TEST);
                yield "Vous êtes accepté suite à l'entretien pour \"" + titre + "\". Des tests vont suivre.";
            }
            case REFUS -> {
                candidatureService.changerStatut(candidature.getId(), StatutCandidature.REFUSEE_ENTRETIEN);
                yield "Votre candidature pour \"" + titre + "\" n'est pas retenue après l'entretien.";
            }
        };
        notificationService.notifier(candidature.getCandidat().getUtilisateur().getId(), message,
                TypeNotification.ENTRETIEN, "/candidat/mes-candidatures");
        return entretien;
    }

    /** Entretiens de la candidature, du premier au dernier (le rang donne le numéro). */
    public List<Entretien> listerPourCandidature(Long candidatureId) {
        return lister(candidatureId);
    }

    /** Vrai si la RH peut planifier un (nouvel) entretien maintenant. */
    public boolean peutPlanifier(Candidature candidature) {
        if (candidature.getStatut() == StatutCandidature.ACCEPTEE_ENTRETIEN) return true;
        if (candidature.getStatut() != StatutCandidature.EN_ENTRETIEN) return false;
        var entretiens = lister(candidature.getId());
        return !entretiens.isEmpty() && entretiens.stream().noneMatch(e -> e.getDecisionRh() == Decision.EN_ATTENTE);
    }

    /** Libellé de la prochaine étape attendue côté RH. */
    public String prochaineEtape(Candidature candidature, boolean offreAvecTests) {
        var entretiens = lister(candidature.getId());
        var enAttente = entretiens.stream().filter(e -> e.getDecisionRh() == Decision.EN_ATTENTE).findFirst();
        return switch (candidature.getStatut()) {
            case RECUE -> "Présélection : retenir pour entretien ou ne pas retenir.";
            case ACCEPTEE_ENTRETIEN -> "Planifier le premier entretien.";
            case EN_ENTRETIEN -> enAttente.isPresent()
                    ? (enAttente.get().getDateHeure().isAfter(LocalDateTime.now())
                        ? "Entretien n° " + (entretiens.indexOf(enAttente.get()) + 1) + " à venir ; décision après sa tenue."
                        : "Décider l’entretien n° " + (entretiens.indexOf(enAttente.get()) + 1) + " : entretien suivant, tests ou refus.")
                    : "Planifier l’entretien n° " + (entretiens.size() + 1) + ".";
            case EN_TEST -> offreAvecTests ? "Faire passer et corriger les tests." : "Créer les tests de l’offre.";
            case EN_EVALUATION -> "Créer puis décider l’évaluation finale.";
            case RETENUE -> candidature.getPropositionStatut() == null
                    ? "Envoyer la proposition d’embauche (contrat, salaire, entrée, rôle)."
                    : switch (candidature.getPropositionStatut()) {
                        case EN_ATTENTE -> "Attendre la réponse du candidat : son acceptation crée automatiquement le dossier salarié.";
                        case ACCEPTEE -> "Proposition acceptée avant l’embauche automatique : finalisez l’embauche ci-dessous.";
                        case REFUSEE -> "Proposition refusée par le candidat : proposez le poste à un autre candidat retenu.";
                    };
            case EMBAUCHE -> "Embauché : suivre le contrat et l’intégration dans le dossier salarié.";
            default -> "Parcours terminé.";
        };
    }

    private List<Entretien> lister(Long candidatureId) {
        return entretienRepository.findByCandidature_IdOrderByDateHeureAscIdAsc(candidatureId);
    }
}
