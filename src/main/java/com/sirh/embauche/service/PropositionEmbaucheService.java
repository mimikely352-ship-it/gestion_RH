package com.sirh.embauche.service;

import com.sirh.commun.enums.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.notification.service.NotificationService;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.entity.OffreEmploi;
import com.sirh.recrutement.repository.CandidatureRepository;
import com.sirh.recrutement.repository.OffreEmploiRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Proposition d'embauche (METIER.md, bloc 3) : envoyée par les RH à une candidature RETENUE, avec toutes les
 * conditions de l'embauche (contrat, salaire, entrée, rôle, service d'accueil). Son acceptation par le candidat
 * crée automatiquement le salarié ({@link EmbaucheService#embaucherALAcceptation}) ; un refus libère le poste
 * pour un autre candidat retenu.
 */
@Service
@RequiredArgsConstructor
public class PropositionEmbaucheService {
    private final CandidatureRepository candidatures;
    private final NotificationService notifications;
    private final com.sirh.demande.repository.DemandeRepository demandes;
    private final OffreEmploiRepository offres;
    private final DossierEmployeRepository dossiers;
    private final EmbaucheService embauche;

    /** Réponse du candidat : en cas d'acceptation, le dossier salarié créé et le compte devenu interne. */
    public record Reponse(boolean embauche, DossierEmploye dossier, Utilisateur utilisateur) {}

    /** Budget annuel restant sur le besoin d'origine de l'offre, hors proposition de cette candidature (null si aucun budget). */
    @Transactional(readOnly = true)
    public BigDecimal budgetRestant(Candidature c) {
        com.sirh.demande.entity.Demande besoin = c.getOffre() == null ? null : c.getOffre().getDemandeOrigine();
        if (besoin == null || besoin.getBudgetGlobalRecrutement() == null) return null;
        var engage = candidatures.salairesMensuelsEngages(besoin.getId(), c.getId());
        return besoin.getBudgetGlobalRecrutement().subtract(annuel(engage));
    }

    private static BigDecimal annuel(BigDecimal mensuel) {
        return (mensuel == null ? BigDecimal.ZERO : mensuel).multiply(BigDecimal.valueOf(12));
    }

    private void verifierBudget(Candidature c, BigDecimal salaire) {
        com.sirh.demande.entity.Demande besoin = c.getOffre() == null ? null : c.getOffre().getDemandeOrigine();
        if (besoin == null) return;
        besoin = demandes.verrouiller(besoin.getId()).orElse(besoin); // sérialise deux envois concurrents
        if (besoin.getBudgetGlobalRecrutement() == null) return;
        var restant = budgetRestant(c);
        if (annuel(salaire).compareTo(restant) > 0)
            throw new BusinessException("Budget global de recrutement dépassé : cette proposition coûte "
                    + annuel(salaire).toPlainString() + " Ar par an, il reste " + restant.max(BigDecimal.ZERO).toPlainString()
                    + " Ar sur " + besoin.getBudgetGlobalRecrutement().toPlainString() + " Ar. Réduisez le salaire ou faites réviser le besoin.");
    }

    /**
     * Postes encore disponibles sur l'offre : postes ouverts − embauches − propositions en attente
     * − propositions acceptées non encore finalisées.
     */
    @Transactional(readOnly = true)
    public long postesDisponibles(OffreEmploi offre) {
        if (offre == null || offre.getId() == null) return 0;
        return offre.getNbPostes()
                - candidatures.countByOffreIdAndStatut(offre.getId(), StatutCandidature.EMBAUCHE)
                - candidatures.countByOffreIdAndPropositionStatut(offre.getId(), StatutProposition.EN_ATTENTE)
                - candidatures.countByOffreIdAndStatutAndPropositionStatut(offre.getId(), StatutCandidature.RETENUE,
                        StatutProposition.ACCEPTEE);
    }

    @Transactional
    public Candidature envoyer(Long id, TypeContrat type, BigDecimal salaire, LocalDate debut, LocalDate fin,
                               Role role, Long serviceId, String auteur) {
        var c = verrouiller(id);
        if (c.getStatut() != StatutCandidature.RETENUE)
            throw new BusinessException("La candidature doit être retenue.");
        if (c.getPropositionStatut() != null)
            throw new BusinessException("Une proposition a déjà été envoyée.");
        if (type == null || salaire == null || salaire.signum() <= 0 || salaire.scale() > 2
                || salaire.precision() - salaire.scale() > 17)
            throw new BusinessException("Indiquez un contrat et un salaire positif à deux décimales maximum.");
        if (debut == null || debut.isBefore(LocalDate.now()))
            throw new BusinessException("La date d’entrée doit être aujourd’hui ou ultérieure.");
        if (type.isDureeDeterminee() && (fin == null || !fin.isAfter(debut)))
            throw new BusinessException("Indiquez une fin postérieure à l’entrée.");
        if (!type.isDureeDeterminee() && fin != null)
            throw new BusinessException("Un CDI ne possède pas de date de fin prévue.");
        var roleAttribue = role == null ? Role.EMPLOYE : role;
        if (!EmbaucheService.ROLES_EMBAUCHE.contains(roleAttribue))
            throw new BusinessException("Le rôle attribué doit être un rôle interne : employé, chef de service ou direction.");
        var service = embauche.serviceAccueil(c, serviceId);
        if (dossiers.findByUtilisateurId(c.getCandidat().getUtilisateur().getId()).isPresent())
            throw new BusinessException("Ce candidat possède déjà un dossier salarié : utilisez un nouveau contrat ou une promotion.");
        if (c.getOffre() != null && c.getOffre().getId() != null) {
            var offre = offres.verrouiller(c.getOffre().getId()).orElse(c.getOffre()); // sérialise les envois sur l'offre
            if (postesDisponibles(offre) <= 0)
                throw new BusinessException("Les " + offre.getNbPostes() + " poste(s) de cette offre sont déjà pourvus ou proposés. "
                        + "Attendez la réponse en cours : un refus libère le poste.");
        }
        verifierBudget(c, salaire);
        c.setPropositionTypeContrat(type);
        c.setPropositionSalaire(salaire);
        c.setPropositionDebut(debut);
        c.setPropositionFin(fin);
        c.setPropositionRole(roleAttribue);
        c.setPropositionServiceId(service.getId());
        c.setPropositionEnvoyeePar(auteur);
        c.setPropositionStatut(StatutProposition.EN_ATTENTE);
        c.setPropositionEnvoyeeLe(LocalDateTime.now());
        notifications.notifier(c.getCandidat().getUtilisateur().getId(), "Une proposition d’embauche vous attend.",
                TypeNotification.EMBAUCHE, "/candidat/propositions/" + id);
        return candidatures.save(c);
    }

    @Transactional(readOnly = true)
    public Candidature consulter(Long id, String email) {
        var c = candidatures.findById(id).orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable."));
        verifierProprietaire(c, email);
        if (c.getPropositionStatut() == null) throw new ResourceNotFoundException("Aucune proposition disponible.");
        return c;
    }

    /**
     * Réponse horodatée et définitive du candidat. L'acceptation crée le salarié dans la même transaction :
     * si l'embauche échoue, la réponse n'est pas enregistrée.
     */
    @Transactional
    public Reponse repondre(Long id, String email, boolean accepter) {
        var c = verrouiller(id);
        verifierProprietaire(c, email);
        if (c.getStatut() != StatutCandidature.RETENUE || c.getPropositionStatut() != StatutProposition.EN_ATTENTE)
            throw new BusinessException("Cette proposition ne peut plus recevoir de réponse.");
        c.setPropositionStatut(accepter ? StatutProposition.ACCEPTEE : StatutProposition.REFUSEE);
        c.setPropositionRepondueLe(LocalDateTime.now());
        candidatures.save(c);
        if (!accepter) {
            notifications.notifierRh("Proposition d’embauche refusée par " + c.getCandidatNomComplet() + " (" + c.getOffreTitre()
                            + ") : vous pouvez proposer le poste à un autre candidat retenu.",
                    TypeNotification.EMBAUCHE, "/rh/candidatures/" + id);
            return new Reponse(false, null, c.getCandidat().getUtilisateur());
        }
        var dossier = embauche.embaucherALAcceptation(c, email);
        return new Reponse(true, dossier, c.getCandidat().getUtilisateur());
    }

    private Candidature verrouiller(Long id) {
        return candidatures.verrouiller(id).orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable."));
    }
    private void verifierProprietaire(Candidature c, String email) {
        if (!c.getCandidat().getUtilisateur().getEmail().equals(email))
            throw new AccessDeniedException("Cette proposition appartient à un autre candidat.");
    }
}
