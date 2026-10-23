package com.sirh.embauche.service;

import com.sirh.commun.enums.*;
import com.sirh.demande.entity.Demande;
import com.sirh.demande.repository.DemandeRepository;
import com.sirh.organisation.entity.Poste;
import com.sirh.organisation.entity.Service;
import com.sirh.recrutement.repository.CandidatureRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AffectationAutomatiqueService {
    private final com.sirh.embauche.repository.DossierEmployeRepository dossiers;
    private final com.sirh.embauche.repository.AffectationRepository affectations;
    private final UtilisateurRepository utilisateurs;

    public static Service serviceDuPoste(Poste poste) {
        return poste == null || poste.getSection() == null ? null : poste.getSection().getService();
    }

    /** Synchronise le service du compte avec l’affectation courante conservée. */
    @Transactional
    public void completer(Utilisateur utilisateur) {
        if (utilisateur.getRole() != Role.EMPLOYE) return;
        var verrouille = utilisateurs.verrouiller(utilisateur.getId()).orElse(null);
        if (verrouille == null) return;
        var dossier = dossiers.findByUtilisateurId(utilisateur.getId()).orElse(null);
        if (dossier == null) return;
        var actuelle = affectations.findByDossierIdAndActuelleTrue(dossier.getId()).orElse(null);
        if (actuelle == null || actuelle.getService() == null) return;
        var service = actuelle.getService();
        if (verrouille.getService() == null || !service.getId().equals(verrouille.getService().getId())) {
            verrouille.setService(service);
            utilisateurs.save(verrouille);
        }
        utilisateur.setService(service);
    }
}
