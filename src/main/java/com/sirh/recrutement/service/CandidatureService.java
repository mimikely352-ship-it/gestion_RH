package com.sirh.recrutement.service;

import com.sirh.candidat.entity.Candidat;
import com.sirh.candidat.repository.CandidatRepository;
import com.sirh.commun.enums.StatutCandidature;
import com.sirh.commun.enums.StatutOffre;
import com.sirh.commun.enums.TypeNotification;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.notification.service.NotificationService;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.entity.OffreEmploi;
import com.sirh.recrutement.repository.CandidatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Gère le dépôt de candidature et le pipeline de statuts qui en découle. */
@Service
@RequiredArgsConstructor
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final CandidatRepository candidatRepository;
    private final OffreEmploiService offreEmploiService;
    private final MatchingService matchingService;
    private final NotificationService notificationService;

    @Transactional
    public Candidature postuler(Long candidatId, Long offreId) {
        Candidat candidat = candidatRepository.findById(candidatId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidat introuvable : " + candidatId));
        OffreEmploi offre = offreEmploiService.obtenir(offreId);

        if (offre.getStatut() != StatutOffre.PUBLIEE) {
            throw new BusinessException("Cette offre n'est pas ouverte aux candidatures.");
        }
        if (offre.getDateLimite() != null && LocalDate.now().isAfter(offre.getDateLimite())) {
            throw new BusinessException("La date limite de candidature est dépassée.");
        }
        if (candidatureRepository.existsByOffreIdAndCandidatId(offreId, candidatId)) {
            throw new BusinessException("Vous avez déjà postulé à cette offre.");
        }

        Candidature candidature = Candidature.builder()
                .offre(offre)
                .candidat(candidat)
                .statut(StatutCandidature.RECUE)
                .build();
        candidature = candidatureRepository.save(candidature);

        BigDecimal score = matchingService.calculerScore(candidat, offre);
        candidature.setScoreMatching(score);

        Long utilisateurId = candidat.getUtilisateur().getId();
        if (matchingService.estAccepteAutomatiquement(score, offre)) {
            candidature.setStatut(StatutCandidature.ACCEPTEE_ENTRETIEN);
            notificationService.notifier(utilisateurId,
                    "Votre candidature pour \"" + offre.getTitre() + "\" est acceptée pour l'entretien.",
                    TypeNotification.CANDIDATURE, "/candidat/mes-candidatures");
        } else {
            candidature.setStatut(StatutCandidature.NON_RETENUE);
            notificationService.notifier(utilisateurId,
                    "Votre candidature pour \"" + offre.getTitre() + "\" n'a pas été retenue.",
                    TypeNotification.CANDIDATURE, "/candidat/mes-candidatures");
        }
        return candidatureRepository.save(candidature);
    }

    public List<Candidature> listerPourCandidat(Long candidatId) {
        return candidatureRepository.findByCandidatId(candidatId);
    }

    public List<Candidature> listerPourOffre(Long offreId) {
        return candidatureRepository.findByOffreId(offreId);
    }

    public Candidature obtenir(Long candidatureId) {
        return candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable : " + candidatureId));
    }

    @Transactional
    public Candidature changerStatut(Long candidatureId, StatutCandidature statut) {
        Candidature candidature = obtenir(candidatureId);
        candidature.setStatut(statut);
        return candidatureRepository.save(candidature);
    }
}
