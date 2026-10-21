package com.sirh.recrutement.service;

import com.sirh.commun.enums.Decision;
import com.sirh.commun.enums.StatutCandidature;
import com.sirh.commun.enums.StatutEntretien;
import com.sirh.commun.enums.TypeNotification;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.notification.service.NotificationService;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.entity.Entretien;
import com.sirh.recrutement.repository.EntretienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EntretienService {

    private final EntretienRepository entretienRepository;
    private final CandidatureService candidatureService;
    private final NotificationService notificationService;

    @Transactional
    public Entretien planifier(Long candidatureId, LocalDateTime dateHeure, String lieu) {
        Candidature candidature = candidatureService.obtenir(candidatureId);
        if (candidature.getStatut() != StatutCandidature.ACCEPTEE_ENTRETIEN) {
            throw new BusinessException("La candidature n'est pas au stade entretien.");
        }
        Entretien entretien = Entretien.builder()
                .candidature(candidature)
                .dateHeure(dateHeure)
                .lieu(lieu)
                .statut(StatutEntretien.PLANIFIE)
                .decisionRh(Decision.EN_ATTENTE)
                .build();
        entretien = entretienRepository.save(entretien);

        candidatureService.changerStatut(candidatureId, StatutCandidature.EN_ENTRETIEN);
        notificationService.notifier(candidature.getCandidat().getUtilisateur().getId(),
                "Un entretien est planifié pour votre candidature \"" + candidature.getOffre().getTitre() + "\".",
                TypeNotification.ENTRETIEN, "/candidat/mes-candidatures");
        return entretien;
    }

    @Transactional
    public Entretien decider(Long entretienId, Decision decision, String commentaire) {
        Entretien entretien = entretienRepository.findById(entretienId)
                .orElseThrow(() -> new ResourceNotFoundException("Entretien introuvable : " + entretienId));
        entretien.setStatut(StatutEntretien.REALISE);
        entretien.setDecisionRh(decision);
        entretien.setCommentaire(commentaire);
        entretienRepository.save(entretien);

        Candidature candidature = entretien.getCandidature();
        StatutCandidature nouveauStatut = decision == Decision.ACCEPTE
                ? StatutCandidature.EN_TEST
                : StatutCandidature.REFUSEE_ENTRETIEN;
        candidatureService.changerStatut(candidature.getId(), nouveauStatut);

        String message = decision == Decision.ACCEPTE
                ? "Vous êtes accepté suite à l'entretien pour \"" + candidature.getOffre().getTitre() + "\"."
                : "Votre candidature pour \"" + candidature.getOffre().getTitre() + "\" n'est pas retenue après l'entretien.";
        notificationService.notifier(candidature.getCandidat().getUtilisateur().getId(), message,
                TypeNotification.ENTRETIEN, "/candidat/mes-candidatures");
        return entretien;
    }

    public List<Entretien> listerPourCandidature(Long candidatureId) {
        return entretienRepository.findByCandidatureId(candidatureId);
    }
}
