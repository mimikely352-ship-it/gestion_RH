package com.sirh.embauche.service;

import com.sirh.commun.enums.Role;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Habilitations sur le dossier salarié (confidentialité) :
 * <ul>
 *   <li>RH : lecture et modification de tout le dossier, journal d'audit ;</li>
 *   <li>DG : lecture de tout le dossier et du journal, sans modification ;</li>
 *   <li>Employé : lecture de son propre dossier (identité, coordonnées, contrats, salaire, documents), sans le journal ;</li>
 *   <li>Chef de service (employés de son service) et Direction : situation professionnelle uniquement
 *       (poste, contrat, intégration, carrière) — ni données personnelles, ni salaire, ni documents ;</li>
 *   <li>tout autre compte : aucun accès.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class HabilitationService {
    private final UtilisateurRepository utilisateurs;

    public record Habilitation(boolean lecture, boolean donneesPersonnelles, boolean salaire, boolean documents,
                               boolean depart, boolean audit, boolean modification) {
        public static final Habilitation AUCUNE = new Habilitation(false, false, false, false, false, false, false);
        public static final Habilitation COMPLETE = new Habilitation(true, true, true, true, true, true, true);
        public static final Habilitation LECTURE_COMPLETE = new Habilitation(true, true, true, true, true, true, false);
        public static final Habilitation TITULAIRE = new Habilitation(true, true, true, true, true, false, false);
        public static final Habilitation PROFESSIONNELLE = new Habilitation(true, false, false, false, false, false, false);

        public boolean isLecture() { return lecture; }
        public boolean isDonneesPersonnelles() { return donneesPersonnelles; }
        public boolean isSalaire() { return salaire; }
        public boolean isDocuments() { return documents; }
        public boolean isDepart() { return depart; }
        public boolean isAudit() { return audit; }
        public boolean isModification() { return modification; }
    }

    public Habilitation pour(Authentication compte, DossierEmploye dossier) {
        if (compte == null || dossier == null) return Habilitation.AUCUNE;
        var lecteur = utilisateurs.findByEmail(compte.getName()).orElse(null);
        return pour(lecteur, dossier);
    }

    public static Habilitation pour(Utilisateur lecteur, DossierEmploye dossier) {
        if (lecteur == null || !lecteur.isActif() || dossier == null || dossier.getUtilisateur() == null) return Habilitation.AUCUNE;
        var titulaire = dossier.getUtilisateur();
        return switch (lecteur.getRole()) {
            case RH -> Habilitation.COMPLETE;
            case DG -> Habilitation.LECTURE_COMPLETE;
            case DIRECTION -> lecteur.getId().equals(titulaire.getId()) ? Habilitation.TITULAIRE : Habilitation.PROFESSIONNELLE;
            case CHEF_SERVICE -> lecteur.getId().equals(titulaire.getId()) ? Habilitation.TITULAIRE
                    : dirigeServiceDe(lecteur, titulaire) ? Habilitation.PROFESSIONNELLE : Habilitation.AUCUNE;
            case EMPLOYE -> lecteur.getId().equals(titulaire.getId()) ? Habilitation.TITULAIRE : Habilitation.AUCUNE;
            default -> Habilitation.AUCUNE;
        };
    }

    private static boolean dirigeServiceDe(Utilisateur chef, Utilisateur employe) {
        var service = employe.getService();
        return service != null && service.getChefService() != null && chef.getId().equals(service.getChefService().getId());
    }

    public static boolean estRole(Authentication compte, Role role) {
        return compte != null && compte.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role.name()));
    }
}
