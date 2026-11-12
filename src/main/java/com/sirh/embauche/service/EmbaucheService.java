package com.sirh.embauche.service;

import com.sirh.commun.enums.ActionAudit;
import com.sirh.commun.enums.Role;
import com.sirh.commun.enums.StatutCandidature;
import com.sirh.commun.enums.StatutProposition;
import com.sirh.commun.enums.TypeNotification;
import com.sirh.commun.exception.BusinessException;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.notification.service.NotificationService;
import com.sirh.organisation.repository.ServiceRepository;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.service.CandidatureService;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Set;

/**
 * Embauche (METIER.md, bloc 3). À l'acceptation de la proposition par le candidat, le système crée
 * automatiquement, en une seule transaction : le compte employé (le compte candidat change de rôle, même login),
 * le dossier salarié et son matricule (EMP-AAAA-0001), l'affectation au poste, le premier contrat aux conditions
 * acceptées (« à venir » si l'entrée est future) et la checklist d'intégration. Si une étape échoue, rien n'est créé.
 */
@Service
@RequiredArgsConstructor
public class EmbaucheService {
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Rôles internes attribuables à l'embauche. */
    public static final Set<Role> ROLES_EMBAUCHE = EnumSet.of(Role.EMPLOYE, Role.CHEF_SERVICE, Role.DIRECTION);

    private final CandidatureService candidatureService;
    private final UtilisateurRepository utilisateurRepository;
    private final ServiceRepository serviceRepository;
    private final NotificationService notificationService;
    private final ParcoursEmployeService parcours;
    private final ContratService contrats;
    private final AuditDossierService audit;

    /** Service d'accueil : celui du poste de l'offre ; à défaut, celui choisi par les RH. */
    public com.sirh.organisation.entity.Service serviceAccueil(Candidature c, Long serviceIdChoisi) {
        var service = c.getOffre() == null ? null : AffectationAutomatiqueService.serviceDuPoste(c.getOffre().getPoste());
        if (service == null && serviceIdChoisi != null)
            service = serviceRepository.findById(serviceIdChoisi)
                    .orElseThrow(() -> new BusinessException("Le service d’accueil choisi n’existe plus."));
        if (service == null)
            throw new BusinessException("Le poste de l’offre n’est rattaché à aucun service : choisissez le service d’accueil.");
        return service;
    }

    /** Création automatique à l'acceptation de la proposition (le candidat est l'acteur). */
    @Transactional
    public DossierEmploye embaucherALAcceptation(Candidature candidature, String acteur) {
        var role = candidature.getPropositionRole() == null ? Role.EMPLOYE : candidature.getPropositionRole();
        var service = serviceAccueil(candidature, candidature.getPropositionServiceId());
        return creer(candidature, role, service, candidature.getPropositionEnvoyeePar(), acteur,
                "acceptation de la proposition par le candidat");
    }

    /**
     * Reprise de l'existant : proposition acceptée avant la création automatique, finalisée par les RH
     * (rôle et service choisis à ce moment).
     */
    @Transactional
    public Candidature finaliser(Long candidatureId, Role roleAttribue, Long serviceId, String acteur) {
        Candidature candidature = candidatureService.obtenirPourModification(candidatureId);
        if (candidature.getStatut() != StatutCandidature.RETENUE)
            throw new BusinessException("Seule une candidature retenue peut être finalisée.");
        if (candidature.getPropositionStatut() != StatutProposition.ACCEPTEE)
            throw new BusinessException("Le candidat doit accepter la proposition avant l’embauche.");
        creer(candidature, roleAttribue, serviceAccueil(candidature, serviceId), acteur, acteur, "finalisation par les RH");
        return candidature;
    }

    private DossierEmploye creer(Candidature candidature, Role role, com.sirh.organisation.entity.Service service,
                                 String auteurConditions, String acteur, String origine) {
        if (candidature.getStatut() != StatutCandidature.RETENUE || candidature.getPropositionStatut() != StatutProposition.ACCEPTEE)
            throw new BusinessException("La proposition doit être acceptée par le candidat avant l’embauche.");
        if (role == null || !ROLES_EMBAUCHE.contains(role))
            throw new BusinessException("Le rôle attribué doit être un rôle interne : employé, chef de service ou direction.");
        if (candidature.getPropositionDebut() == null || candidature.getPropositionTypeContrat() == null)
            throw new BusinessException("Les conditions de la proposition sont incomplètes (contrat, date d’entrée).");

        Utilisateur utilisateur = candidature.getCandidat().getUtilisateur();
        utilisateurRepository.verrouiller(utilisateur.getId())
                .orElseThrow(() -> new BusinessException("Compte introuvable."));
        utilisateur.setRole(role);
        utilisateur.setService(service);
        utilisateurRepository.save(utilisateur);

        candidature.setDateEntree(candidature.getPropositionDebut());
        var dossier = parcours.embaucher(candidature, service);
        String auteur = auteurConditions == null || auteurConditions.isBlank() ? acteur : auteurConditions;
        var contrat = contrats.ajouter(dossier.getId(), candidature.getPropositionTypeContrat(),
                candidature.getPropositionDebut(), candidature.getPropositionFin(), candidature.getPropositionSalaire(),
                null, auteur);
        candidatureService.changerStatut(candidature.getId(), StatutCandidature.EMBAUCHE);

        String entree = candidature.getPropositionDebut().format(JOUR);
        String offre = candidature.getOffre() == null ? "offre" : candidature.getOffre().getTitre();
        audit.journaliserDansTransaction(dossier.getId(), acteur, null, ActionAudit.EMBAUCHE,
                "Matricule " + dossier.getMatricule() + " — " + offre + " (candidature n°" + candidature.getId() + ", "
                        + origine + ") ; contrat " + contrat.getType().name() + " à partir du " + entree
                        + " aux conditions envoyées par " + auteur);
        notificationService.notifier(utilisateur.getId(), "Bienvenue ! Votre embauche est confirmée : dossier salarié "
                        + dossier.getMatricule() + ", entrée le " + entree + ". Votre espace salarié est accessible avec vos identifiants habituels.",
                TypeNotification.EMBAUCHE, "/mon-dossier");
        String poste = candidature.getOffre() == null || candidature.getOffre().getPoste() == null ? ""
                : " (" + candidature.getOffre().getPoste().getIntitule() + ")";
        notificationService.notifierRh("Nouvel employé : " + dossier.getMatricule() + " — " + dossier.getPrenom() + " "
                        + dossier.getNom() + poste + ", entrée le " + entree + ".",
                TypeNotification.EMBAUCHE, "/rh/employes/" + dossier.getId());
        return dossier;
    }
}
