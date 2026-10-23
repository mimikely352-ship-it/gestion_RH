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
    private final com.sirh.candidat.repository.DocumentRepository documentRepository;
    private final OffreEmploiService offreEmploiService;
    private final MatchingService matchingService;
    private final NotificationService notificationService;

    @Transactional
    public Candidature postuler(Long candidatId, Long offreId) {
        return postulerAvecPieces(candidatId, offreId, java.util.Set.of());
    }

    @Transactional
    public Candidature postulerAvecPieces(Long candidatId, Long offreId,
            java.util.Set<com.sirh.candidat.entity.Document> pieces) {
        // Sérialise les dépôts du même candidat, y compris sur deux offres différentes.
        Candidat candidat = candidatRepository.verrouiller(candidatId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidat introuvable : " + candidatId));
        OffreEmploi offre = offreEmploiService.obtenir(offreId);

        if (candidat.getUtilisateur().getRole() != com.sirh.commun.enums.Role.CANDIDAT)
            throw new BusinessException("Un employé doit utiliser une demande de promotion ou de transfert.");
        if (offre.getEntreprise() == null)
            throw new BusinessException("L’entreprise de cette offre doit être renseignée par les RH avant le dépôt.");

        if (offre.getStatut() != StatutOffre.PUBLIEE) {
            throw new BusinessException("Cette offre n'est pas ouverte aux candidatures.");
        }
        if (offre.getDateLimite() != null && LocalDate.now().isAfter(offre.getDateLimite())) {
            throw new BusinessException("La date limite de candidature est dépassée.");
        }
        if (candidatureRepository.existsByOffreIdAndCandidatId(offreId, candidatId)) {
            throw new BusinessException("Vous avez déjà postulé à cette offre.");
        }
        if (candidatureRepository.existsByCandidatIdAndOffreEntrepriseId(candidatId, offre.getEntreprise().getId()))
            throw new BusinessException("Vous avez déjà candidaté auprès de cette entreprise. Un seul poste par entreprise est autorisé.");
        var libelles = new java.util.HashSet<String>();
        for (var piece : pieces) {
            if (piece.getCandidat() == null || !candidatId.equals(piece.getCandidat().getId()))
                throw new BusinessException("Une pièce jointe n'appartient pas à votre dossier.");
            libelles.add(piece.getLibelle());
        }
        if (!libelles.containsAll(offre.getPiecesRequises()))
            throw new BusinessException("Joignez toutes les pièces demandées depuis la page de l'offre.");

        Candidature candidature = Candidature.builder()
                .offre(offre)
                .candidat(candidat)
                .cvDocument(documentRepository.findFirstByCandidatIdAndTypeOrderByDateUploadDesc(candidatId, "CV").orElse(null))
                .diplomes(new java.util.HashSet<>(candidat.getDiplomes()))
                .piecesComplementaires(new java.util.HashSet<>(pieces))
                .statut(StatutCandidature.RECUE)
                .build();
        candidature = candidatureRepository.save(candidature);

        BigDecimal score = matchingService.calculerScore(candidat, offre);
        candidature.setScoreMatching(score);

        notificationService.notifier(candidat.getUtilisateur().getId(),
                "Votre candidature a été reçue et sera examinée par les RH.",
                TypeNotification.CANDIDATURE, "/candidat/mes-candidatures");
        String titreCourt = offre.getTitre().substring(0, Math.min(120, offre.getTitre().length()));
        notificationService.notifierRh("Nouvelle candidature pour « " + titreCourt + " ». Dossier à examiner.",
                TypeNotification.CANDIDATURE, "/rh/candidatures/" + candidature.getId());
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
        boolean autorise = switch (candidature.getStatut()) {
            case RECUE -> statut == StatutCandidature.ACCEPTEE_ENTRETIEN || statut == StatutCandidature.NON_RETENUE;
            case ACCEPTEE_ENTRETIEN -> statut == StatutCandidature.EN_ENTRETIEN;
            case EN_ENTRETIEN -> statut == StatutCandidature.EN_TEST || statut == StatutCandidature.REFUSEE_ENTRETIEN;
            case EN_TEST -> statut == StatutCandidature.EN_EVALUATION || statut == StatutCandidature.REFUSEE;
            case EN_EVALUATION -> statut == StatutCandidature.RETENUE || statut == StatutCandidature.REFUSEE;
            case RETENUE -> statut == StatutCandidature.EMBAUCHE;
            default -> false;
        };
        if (!autorise) throw new BusinessException("Transition de candidature non autorisée.");
        boolean preselection = candidature.getStatut() == StatutCandidature.RECUE;
        candidature.setStatut(statut);
        if (preselection) {
            String decision = statut == StatutCandidature.ACCEPTEE_ENTRETIEN
                    ? "retenue pour un entretien" : "non retenue";
            notificationService.notifier(candidature.getCandidat().getUtilisateur().getId(),
                    "Votre candidature pour \"" + candidature.getOffre().getTitre() + "\" est " + decision + ".",
                    TypeNotification.CANDIDATURE, "/candidat/mes-candidatures");
        }
        return candidatureRepository.save(candidature);
    }
}
