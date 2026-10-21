package com.sirh.demande.service;

import com.sirh.commun.enums.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.demande.entity.Demande;
import com.sirh.demande.entity.ValidationDemande;
import com.sirh.demande.repository.DemandeRepository;
import com.sirh.demande.repository.ValidationDemandeRepository;
import com.sirh.notification.service.NotificationService;
import com.sirh.organisation.entity.Poste;
import com.sirh.organisation.repository.PosteRepository;
import com.sirh.recrutement.repository.OffreEmploiRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Circuit hiérarchique des demandes internes : Employé → Chef de service → Direction → RH → DG.
 * Chaque étape de validation est historisée ; un rejet à n'importe quelle étape clôt le circuit.
 */
@Service
@RequiredArgsConstructor
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final ValidationDemandeRepository validationDemandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PosteRepository posteRepository;
    private final OffreEmploiRepository offreEmploiRepository;
    private final NotificationService notificationService;

    @Value("${app.demande.delai-execution-jours-defaut}")
    private int delaiExecutionJoursDefaut;

    @Transactional
    public Demande creer(Long auteurId, TypeDemande type, Long posteCibleId, String description) {
        Utilisateur auteur = utilisateurRepository.findById(auteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + auteurId));
        if (auteur.getService() == null) {
            throw new BusinessException("Impossible de créer une demande : aucun service de rattachement.");
        }
        if (auteur.getService().getChefService() == null) {
            throw new BusinessException("Ce service n'a pas de chef de service désigné pour valider la demande.");
        }

        Poste posteCible = null;
        if (posteCibleId != null) {
            posteCible = posteRepository.findById(posteCibleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Poste introuvable : " + posteCibleId));
        }

        if (type == TypeDemande.TRANSFERT) {
            if (posteCible == null) {
                throw new BusinessException("Un transfert doit cibler un poste.");
            }
            final Long idPosteCible = posteCible.getId();
            boolean offrePubliee = offreEmploiRepository.findByStatut(StatutOffre.PUBLIEE).stream()
                    .anyMatch(o -> o.getPoste().getId().equals(idPosteCible));
            if (!offrePubliee) {
                throw new BusinessException("Un transfert n'est possible que s'il existe une offre publiée pour ce poste.");
            }
        }

        Demande demande = Demande.builder()
                .type(type)
                .auteur(auteur)
                .posteCible(posteCible)
                .description(description)
                .statut(StatutDemande.SOUMISE)
                .build();
        demande = demandeRepository.save(demande);

        notificationService.notifier(auteur.getService().getChefService().getId(),
                "Nouvelle demande (" + type + ") de " + auteur.getEmail() + " à valider.",
                TypeNotification.DEMANDE, "/chef-service/demandes");
        return demande;
    }

    @Transactional
    public Demande valider(Long demandeId, Long valideurId, DecisionValidation decision, String commentaire) {
        Demande demande = obtenir(demandeId);
        Utilisateur valideur = utilisateurRepository.findById(valideurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + valideurId));

        EtapeValidation etape = etapeAttendue(demande.getStatut());
        verifierHabilitation(demande, valideur, etape);

        validationDemandeRepository.save(ValidationDemande.builder()
                .demande(demande)
                .etape(etape)
                .valideur(valideur)
                .decision(decision)
                .commentaire(commentaire)
                .build());

        if (decision == DecisionValidation.REJETE) {
            demande.setStatut(StatutDemande.REJETEE);
            demandeRepository.save(demande);
            notificationService.notifier(demande.getAuteur().getId(),
                    "Votre demande a été rejetée à l'étape " + etape + ".",
                    TypeNotification.DEMANDE, "/employe/demandes");
            return demande;
        }

        switch (etape) {
            case CHEF_SERVICE -> avancerVers(demande, StatutDemande.VALIDEE_CHEF, roleValideurSuivant(demande));
            case DIRECTION -> avancerVers(demande, StatutDemande.VALIDEE_DIRECTION, roleValideurSuivant(demande));
            case RH -> avancerVers(demande, StatutDemande.VALIDEE_RH, roleValideurSuivant(demande));
            case DG -> {
                demande.setStatut(StatutDemande.VALIDEE_DG);
                demande.setDateLimiteExecution(LocalDateTime.now().plusDays(delaiExecutionJoursDefaut));
                demandeRepository.save(demande);
                notifierRhPourExecution(demande);
            }
        }
        return demande;
    }

    @Transactional
    public Demande marquerEnExecution(Long demandeId) {
        Demande demande = obtenir(demandeId);
        if (demande.getStatut() != StatutDemande.VALIDEE_DG) {
            throw new BusinessException("La demande doit être validée par le DG avant exécution.");
        }
        demande.setStatut(StatutDemande.EN_EXECUTION);
        return demandeRepository.save(demande);
    }

    @Transactional
    public Demande marquerExecutee(Long demandeId) {
        Demande demande = obtenir(demandeId);
        demande.setStatut(StatutDemande.EXECUTEE);
        demande = demandeRepository.save(demande);
        notificationService.notifier(demande.getAuteur().getId(),
                "Votre demande a été exécutée.", TypeNotification.DEMANDE, "/employe/demandes");
        return demande;
    }

    public Demande obtenir(Long id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable : " + id));
    }

    public List<Demande> listerPourAuteur(Long auteurId) {
        return demandeRepository.findByAuteurId(auteurId);
    }

    public List<ValidationDemande> historique(Long demandeId) {
        return validationDemandeRepository.findByDemandeIdOrderByDateValidationAsc(demandeId);
    }

    private EtapeValidation etapeAttendue(StatutDemande statut) {
        return switch (statut) {
            case SOUMISE -> EtapeValidation.CHEF_SERVICE;
            case VALIDEE_CHEF -> EtapeValidation.DIRECTION;
            case VALIDEE_DIRECTION -> EtapeValidation.RH;
            case VALIDEE_RH -> EtapeValidation.DG;
            default -> throw new BusinessException("Cette demande n'est plus en attente de validation (statut " + statut + ").");
        };
    }

    private void verifierHabilitation(Demande demande, Utilisateur valideur, EtapeValidation etape) {
        boolean habilite = switch (etape) {
            case CHEF_SERVICE -> valideur.getRole() == Role.CHEF_SERVICE
                    && demande.getAuteur().getService() != null
                    && valideur.equals(demande.getAuteur().getService().getChefService());
            case DIRECTION -> valideur.getRole() == Role.DIRECTION;
            case RH -> valideur.getRole() == Role.RH;
            case DG -> valideur.getRole() == Role.DG;
        };
        if (!habilite) {
            throw new BusinessException("Vous n'êtes pas habilité à valider cette demande à l'étape " + etape + ".");
        }
    }

    private Role roleValideurSuivant(Demande demande) {
        return switch (demande.getStatut()) {
            case SOUMISE -> Role.DIRECTION;
            case VALIDEE_CHEF -> Role.RH;
            case VALIDEE_DIRECTION -> Role.DG;
            default -> null;
        };
    }

    private void avancerVers(Demande demande, StatutDemande nouveauStatut, Role roleAPrevenir) {
        demande.setStatut(nouveauStatut);
        demandeRepository.save(demande);
        if (roleAPrevenir != null) {
            utilisateurRepository.findByRole(roleAPrevenir)
                    .forEach(u -> notificationService.notifier(u.getId(),
                            "Une demande (" + demande.getType() + ") attend votre validation.",
                            TypeNotification.DEMANDE, "/demandes/" + demande.getId()));
        }
    }

    private void notifierRhPourExecution(Demande demande) {
        utilisateurRepository.findByRole(Role.RH)
                .forEach(u -> notificationService.notifier(u.getId(),
                        "Demande validée par le DG, à exécuter avant " + demande.getDateLimiteExecution() + ".",
                        TypeNotification.DEMANDE, "/rh/demandes/" + demande.getId()));
    }
}
