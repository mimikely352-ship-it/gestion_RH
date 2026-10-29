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
 * Transferts : décision RH directe. Autres demandes : Chef → Direction → RH → DG.
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
    private final com.sirh.embauche.service.AffectationAutomatiqueService affectationAutomatique;
    private final com.sirh.embauche.repository.DossierEmployeRepository dossiers;
    private final com.sirh.embauche.repository.AffectationRepository affectations;
    private final com.sirh.embauche.service.ParcoursEmployeService parcours;

    @Value("${app.demande.delai-execution-jours-defaut}")
    private int delaiExecutionJoursDefaut;

    @Transactional
    public Demande creer(Long auteurId, TypeDemande type, Long posteCibleId, String description) {
        Utilisateur auteur = utilisateurRepository.verrouiller(auteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + auteurId));
        affectationAutomatique.completer(auteur);
        if (type == null || description == null || description.isBlank() || description.length() > 10000)
            throw new BusinessException("Précisez le type et un motif de 1 à 10 000 caractères.");
        if (type != TypeDemande.TRANSFERT && auteur.getService() == null) {
            throw new BusinessException("Impossible de créer une demande : aucun service de rattachement.");
        }
        if (type != TypeDemande.TRANSFERT && auteur.getService().getChefService() == null) {
            throw new BusinessException("Ce service n'a pas de chef de service désigné pour valider la demande.");
        }

        Poste posteCible = null;
        if (posteCibleId != null) {
            posteCible = posteRepository.findById(posteCibleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Poste introuvable : " + posteCibleId));
        }

        Long origine = null;
        if (type == TypeDemande.TRANSFERT || type == TypeDemande.PROMOTION) {
            if (posteCible == null) {
                throw new BusinessException("Un transfert doit cibler un poste.");
            }
            if (!posteCible.isMobiliteOuverte() || com.sirh.embauche.service.AffectationAutomatiqueService.serviceDuPoste(posteCible) == null)
                throw new BusinessException("Ce poste n’est pas ouvert à la mobilité interne ou son service manque.");
            var dossier = dossiers.findByUtilisateurId(auteurId)
                    .orElseThrow(() -> new BusinessException("Votre dossier employé doit être complété par les RH."));
            var actuelle = affectations.findByDossierIdAndActuelleTrue(dossier.getId())
                    .orElseThrow(() -> new BusinessException("Votre affectation actuelle doit être complétée par les RH."));
            if (actuelle.getPoste().getId().equals(posteCible.getId()))
                throw new BusinessException("Vous occupez déjà ce poste.");
            boolean enCours = demandeRepository.findByAuteurId(auteurId).stream().anyMatch(d ->
                    (d.getType() == TypeDemande.TRANSFERT || d.getType() == TypeDemande.PROMOTION)
                    && d.getStatut() != StatutDemande.REJETEE && d.getStatut() != StatutDemande.EXECUTEE);
            if (enCours) throw new BusinessException("Vous avez déjà une demande de mouvement en cours.");
            origine = actuelle.getId();
        }

        Demande demande = Demande.builder()
                .type(type)
                .auteur(auteur)
                .posteCible(posteCible)
                .description(description)
                .statut(StatutDemande.SOUMISE)
                .affectationOrigineId(origine)
                .build();
        demande = demandeRepository.save(demande);

        if (type == TypeDemande.TRANSFERT) {
            notificationService.notifierRh("Nouvelle demande de transfert de " + auteur.getEmail() + " à examiner.",
                    TypeNotification.DEMANDE, "/rh/demandes");
        } else notificationService.notifier(auteur.getService().getChefService().getId(),
                "Nouvelle demande (" + type + ") de " + auteur.getEmail() + " à valider.",
                TypeNotification.DEMANDE, "/chef-service/demandes");
        return demande;
    }

    @Transactional
    public Demande valider(Long demandeId, Long valideurId, DecisionValidation decision, String commentaire) {
        return valider(demandeId, valideurId, decision, commentaire, null);
    }

    /**
     * @param nouveauPosteCibleId poste final choisi par le RH, qui peut différer du poste demandé par l'employé
     *                            (un autre poste, un poste de chef de service dans un autre département, etc.).
     *                            Uniquement pris en compte à l'étape de validation RH, sur un transfert ou une
     *                            promotion ; le DG valide ensuite ce poste final, pas celui demandé initialement.
     */
    @Transactional
    public Demande valider(Long demandeId, Long valideurId, DecisionValidation decision, String commentaire, Long nouveauPosteCibleId) {
        Demande demande = obtenir(demandeId);
        Utilisateur valideur = utilisateurRepository.findById(valideurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + valideurId));

        EtapeValidation etape = transfertAExaminer(demande) ? EtapeValidation.RH : etapeAttendue(demande.getStatut());
        if (demande.getType() == TypeDemande.TRANSFERT && !transfertAExaminer(demande)) {
            throw new BusinessException("Ce transfert a déjà fait l’objet d’une décision RH.");
        }
        verifierHabilitation(demande, valideur, etape);
        if (decision == null) throw new BusinessException("Précisez une décision.");
        if (decision == DecisionValidation.VALIDE && (demande.getType() == TypeDemande.TRANSFERT || demande.getType() == TypeDemande.PROMOTION)) {
            utilisateurRepository.verrouiller(demande.getAuteur().getId()).orElseThrow(() -> new BusinessException("Compte introuvable."));
            var dossier = dossiers.findByUtilisateurId(demande.getAuteur().getId()).orElseThrow(() -> new BusinessException("Dossier employé manquant."));
            var actuelle = affectations.findByDossierIdAndActuelleTrue(dossier.getId()).orElseThrow(() -> new BusinessException("Affectation actuelle manquante."));
            if (!actuelle.getId().equals(demande.getAffectationOrigineId()))
                throw new BusinessException("L’affectation a changé depuis la demande. Déposez une nouvelle demande.");
            if (nouveauPosteCibleId != null) {
                if (etape != EtapeValidation.RH)
                    throw new BusinessException("Le poste cible ne peut être modifié qu'à l'étape de validation RH.");
                Poste nouveauPoste = posteRepository.findById(nouveauPosteCibleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Poste introuvable : " + nouveauPosteCibleId));
                if (!nouveauPoste.isMobiliteOuverte() || com.sirh.embauche.service.AffectationAutomatiqueService.serviceDuPoste(nouveauPoste) == null)
                    throw new BusinessException("Ce poste n’est pas ouvert à la mobilité interne ou son service manque.");
                if (actuelle.getPoste().getId().equals(nouveauPoste.getId()))
                    throw new BusinessException("L’employé occupe déjà ce poste.");
                demande.setPosteCible(nouveauPoste);
            }
            if (!demande.getPosteCible().isMobiliteOuverte()) throw new BusinessException("Le poste n’est plus ouvert à la mobilité.");
        }

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

        if (demande.getType() == TypeDemande.TRANSFERT) {
            demande.setStatut(StatutDemande.VALIDEE_RH);
            demande.setDateLimiteExecution(LocalDateTime.now().plusDays(delaiExecutionJoursDefaut));
            demandeRepository.save(demande);
            notificationService.notifier(demande.getAuteur().getId(), "Votre transfert a été accepté par les RH. Votre poste reste inchangé jusqu’à sa réalisation.",
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
        if (!preteAExecuter(demande)) {
            throw new BusinessException("La demande doit être approuvée avant exécution.");
        }
        demande.setStatut(StatutDemande.EN_EXECUTION);
        return demandeRepository.save(demande);
    }

    @Transactional
    public Demande marquerExecutee(Long demandeId) {
        return marquerExecutee(demandeId, java.time.LocalDate.now());
    }

    @Transactional
    public Demande marquerExecutee(Long demandeId, java.time.LocalDate dateEffet) {
        Demande demande = obtenir(demandeId);
        if (!preteAExecuter(demande) && demande.getStatut() != StatutDemande.EN_EXECUTION) {
            throw new BusinessException("La demande doit être validée avant son exécution.");
        }
        demande.setDateExecution(LocalDateTime.now());
        if (dateEffet == null || dateEffet.isAfter(java.time.LocalDate.now())
                || (demande.getDateCreation() != null && dateEffet.isBefore(demande.getDateCreation().toLocalDate())))
            throw new BusinessException("La date de réalisation doit être comprise entre la demande et aujourd’hui.");
        demande.setDateEffet(dateEffet);
        if (demande.getType() == TypeDemande.TRANSFERT || demande.getType() == TypeDemande.PROMOTION) {
            parcours.appliquerMouvement(demande, dateEffet);
            utilisateurRepository.save(demande.getAuteur());
        }
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

    public List<Demande> listerPourValidationRh() {
        return demandeRepository.aValiderParRh();
    }

    public List<Demande> listerPourExecutionRh() {
        return demandeRepository.aExecuterParRh();
    }

    private static boolean transfertAExaminer(Demande d) {
        return d.getType() == TypeDemande.TRANSFERT && (d.getStatut() == StatutDemande.SOUMISE
                || d.getStatut() == StatutDemande.VALIDEE_CHEF || d.getStatut() == StatutDemande.VALIDEE_DIRECTION);
    }

    private static boolean preteAExecuter(Demande d) {
        return d.getStatut() == StatutDemande.VALIDEE_DG
                || (d.getType() == TypeDemande.TRANSFERT && d.getStatut() == StatutDemande.VALIDEE_RH);
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
                            TypeNotification.DEMANDE, switch (roleAPrevenir) {
                                case DIRECTION -> "/direction/demandes";
                                case RH -> "/rh/demandes";
                                case DG -> "/dg/demandes";
                                default -> "/chef-service/demandes";
                            }));
        }
    }

    private void notifierRhPourExecution(Demande demande) {
        utilisateurRepository.findByRole(Role.RH)
                .forEach(u -> notificationService.notifier(u.getId(),
                        "Demande validée par le DG, à exécuter avant " + demande.getDateLimiteExecution() + ".",
                        TypeNotification.DEMANDE, "/rh/demandes/" + demande.getId()));
    }
}
