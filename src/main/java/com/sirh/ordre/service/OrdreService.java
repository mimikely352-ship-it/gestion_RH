package com.sirh.ordre.service;

import com.sirh.commun.enums.Role;
import com.sirh.commun.enums.StatutOrdre;
import com.sirh.commun.enums.TypeNotification;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.notification.service.NotificationService;
import com.sirh.ordre.entity.Ordre;
import com.sirh.ordre.repository.OrdreRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Ordres directs du DG vers un RH : pas de circuit de validation, notification immédiate. */
@Service
@RequiredArgsConstructor
public class OrdreService {

    private final OrdreRepository ordreRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;

    @Transactional
    public Ordre emettre(Long dgId, Long rhId, String description, int delaiJours) {
        if (delaiJours < 1 || delaiJours > 365 || description == null || description.isBlank() || description.length() > 10000)
            throw new BusinessException("Précisez une instruction et un délai compris entre 1 et 365 jours.");
        Utilisateur dg = utilisateurRepository.findById(dgId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + dgId));
        if (dg.getRole() != Role.DG) {
            throw new BusinessException("Seul le DG peut émettre un ordre.");
        }
        Utilisateur rh = utilisateurRepository.findById(rhId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + rhId));
        if (rh.getRole() != Role.RH) {
            throw new BusinessException("Un ordre ne peut être destiné qu'à un utilisateur RH.");
        }

        LocalDateTime maintenant = LocalDateTime.now();
        Ordre ordre = Ordre.builder()
                .dg(dg)
                .rh(rh)
                .description(description)
                .delaiJours(delaiJours)
                .dateEmission(maintenant)
                .dateLimite(maintenant.plusDays(delaiJours))
                .statut(StatutOrdre.EMIS)
                .build();
        ordre = ordreRepository.save(ordre);

        notificationService.notifier(rhId,
                "Nouvel ordre du DG à exécuter avant " + ordre.getDateLimite() + " : " + description,
                TypeNotification.ORDRE, "/rh/ordres/" + ordre.getId());
        return ordre;
    }

    @Transactional
    public Ordre marquerEnExecution(Long ordreId, String email) {
        Ordre ordre = obtenir(ordreId);
        verifierDestinataire(ordre, email);
        if (ordre.getStatut() != StatutOrdre.EMIS) throw new BusinessException("Cet ordre n’est plus en attente d’exécution.");
        ordre.setStatut(StatutOrdre.EN_EXECUTION);
        return ordreRepository.save(ordre);
    }

    @Transactional
    public Ordre marquerExecute(Long ordreId, String email) {
        Ordre ordre = obtenir(ordreId);
        verifierDestinataire(ordre, email);
        if (ordre.getStatut() != StatutOrdre.EN_EXECUTION) throw new BusinessException("Démarrez l’exécution avant de la terminer.");
        ordre.setStatut(StatutOrdre.EXECUTE);
        ordre = ordreRepository.save(ordre);
        notificationService.notifier(ordre.getDg().getId(),
                "L'ordre \"" + ordre.getDescription() + "\" a été exécuté par le RH.",
                TypeNotification.ORDRE, "/dg/ordres/" + ordre.getId());
        return ordre;
    }

    public Ordre obtenir(Long id) {
        return ordreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordre introuvable : " + id));
    }

    private void verifierDestinataire(Ordre ordre, String email) {
        if (!ordre.getRh().getEmail().equals(email))
            throw new org.springframework.security.access.AccessDeniedException("Cet ordre est destiné à un autre compte RH.");
    }

    public List<Ordre> listerPourRh(Long rhId) {
        return ordreRepository.findByRhId(rhId);
    }
}
