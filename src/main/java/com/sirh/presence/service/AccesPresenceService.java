package com.sirh.presence.service;
import com.sirh.commun.enums.*;
import com.sirh.commun.exception.*;
import com.sirh.embauche.entity.*;
import com.sirh.embauche.repository.*;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class AccesPresenceService {
    private final UtilisateurRepository utilisateurs;
    private final DossierEmployeRepository dossiers;
    private final AuditDossierRepository journal;
    private final java.time.Clock horloge;
    public Utilisateur utilisateur(Authentication auth) {
        var u = utilisateurs.findByEmail(auth.getName()).orElseThrow(() -> new AccessDeniedException("Compte introuvable."));
        if (!u.isActif() || u.getRole() == Role.CANDIDAT) throw new AccessDeniedException("Compte interne actif requis.");
        return u;
    }
    public Utilisateur exigerRh(Authentication auth) {
        var u = utilisateur(auth);
        if (u.getRole() != Role.RH) throw new AccessDeniedException("Accès RH requis.");
        return u;
    }
    public DossierEmploye personnel(Authentication auth) {
        return dossiers.findByUtilisateurId(utilisateur(auth).getId())
                .orElseThrow(() -> new BusinessException("Votre dossier salarié doit être complété par les RH."));
    }
    public DossierEmploye lire(Long id, Authentication auth) {
        var u = utilisateur(auth);
        var d = dossier(id);
        if (u.getRole() != Role.RH && !d.getUtilisateur().getId().equals(u.getId()))
            throw new AccessDeniedException("Ce dossier ne vous appartient pas.");
        return d;
    }
    public DossierEmploye dossier(Long id) {
        return dossiers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable."));
    }
    public DossierEmploye verrouiller(Long id) {
        return dossiers.verrouillerParId(id).orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable."));
    }
    public void exigerActif(DossierEmploye d) {
        if (!d.getUtilisateur().isActif()) throw new BusinessException("Ce salarié a quitté l’entreprise.");
    }
    public Long responsable(DossierEmploye d) {
        var service = d.getUtilisateur().getService();
        var chef = service == null ? null : service.getChefService();
        return chef != null && chef.isActif() && chef.getRole() == Role.CHEF_SERVICE
                && !chef.getId().equals(d.getUtilisateur().getId()) ? chef.getId() : null;
    }
    public void tracer(Long id, Authentication auth, ActionAudit action, String detail) {
        journal.save(AuditDossier.builder().dossierId(id).acteur(auth.getName()).role(utilisateur(auth).getRole().name())
                .horodatage(LocalDateTime.now(horloge)).action(action).detail(detail.substring(0, Math.min(500, detail.length()))).build());
    }
}
