package com.sirh.embauche.service;

import com.sirh.commun.enums.Role;
import com.sirh.commun.enums.StatutCandidature;
import com.sirh.commun.enums.TypeNotification;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.notification.service.NotificationService;
import com.sirh.organisation.repository.ServiceRepository;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.service.CandidatureService;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acceptation finale : le RH attribue le rôle/poste au candidat retenu. On réutilise le compte
 * utilisateur déjà créé à l'inscription (même login) en faisant évoluer son rôle de CANDIDAT vers
 * le rôle interne attribué, plutôt que de créer un second compte.
 */
@Service
@RequiredArgsConstructor
public class EmbaucheService {

    private final CandidatureService candidatureService;
    private final UtilisateurRepository utilisateurRepository;
    private final ServiceRepository serviceRepository;
    private final NotificationService notificationService;
    private final ParcoursEmployeService parcours;

    @Transactional
    public Candidature finaliser(Long candidatureId, Role roleAttribue, Long serviceId) {
        Candidature candidature = candidatureService.obtenir(candidatureId);
        if (candidature.getStatut() != StatutCandidature.RETENUE) {
            throw new BusinessException("Seule une candidature retenue peut être finalisée.");
        }
        if (roleAttribue != Role.EMPLOYE && roleAttribue != Role.CHEF_SERVICE && roleAttribue != Role.DIRECTION) {
            throw new BusinessException("Le rôle attribué doit être un rôle interne (EMPLOYE, CHEF_SERVICE, DIRECTION...).");
        }

        var service = AffectationAutomatiqueService.serviceDuPoste(candidature.getOffre().getPoste());
        if (service == null && serviceId != null) {
            service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service introuvable : " + serviceId));
        }
        if (service == null) throw new BusinessException("Le poste de l’offre doit être associé à une section et à un service avant l’embauche.");

        Utilisateur utilisateur = candidature.getCandidat().getUtilisateur();
        utilisateurRepository.verrouiller(utilisateur.getId())
                .orElseThrow(() -> new BusinessException("Compte introuvable."));
        utilisateur.setRole(roleAttribue);
        utilisateur.setService(service);
        utilisateurRepository.save(utilisateur);

        candidature.setDateEntree(java.time.LocalDate.now());
        parcours.embaucher(candidature, service);
        candidature = candidatureService.changerStatut(candidatureId, StatutCandidature.EMBAUCHE);

        notificationService.notifier(utilisateur.getId(),
                "Félicitations, vous êtes embauché(e) ! Connectez-vous avec votre email et votre mot de passe habituels.",
                TypeNotification.EMBAUCHE, "/login");
        return candidature;
    }
}
