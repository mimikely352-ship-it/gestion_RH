package com.sirh.embauche.service;

import com.sirh.embauche.entity.*;
import com.sirh.embauche.repository.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.demande.entity.Demande;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service @RequiredArgsConstructor
public class ParcoursEmployeService {
    private final DossierEmployeRepository dossiers;
    private final AffectationRepository affectations;

    @Transactional
    public void embaucher(Candidature c, com.sirh.organisation.entity.Service service) {
        var utilisateur = c.getCandidat().getUtilisateur();
        if (dossiers.findByUtilisateurId(utilisateur.getId()).isPresent())
            throw new BusinessException("Cet utilisateur possède déjà un dossier employé. Utilisez un mouvement interne.");
        var dossier = dossiers.save(DossierEmploye.builder().utilisateur(utilisateur)
                .nom(c.getCandidat().getNom()).prenom(c.getCandidat().getPrenom()).dateEntree(c.getDateEntree()).build());
        enregistrer(dossier, c.getOffre().getPoste(), service, c.getDateEntree(), "EMBAUCHE", c.getId());
    }

    @Transactional
    public void appliquerMouvement(Demande demande, LocalDate effet) {
        var dossier = dossiers.verrouillerParUtilisateur(demande.getAuteur().getId())
                .orElseThrow(() -> new BusinessException("Le dossier employé doit être complété avant le mouvement."));
        var actuelle = affectations.findByDossierIdAndActuelleTrue(dossier.getId())
                .orElseThrow(() -> new BusinessException("L’affectation actuelle doit être renseignée avant le mouvement."));
        if (!actuelle.getId().equals(demande.getAffectationOrigineId()))
            throw new BusinessException("L’affectation a changé depuis la demande. Déposez une nouvelle demande.");
        if (effet == null || effet.isAfter(LocalDate.now()) || (actuelle.getDateDebut() != null && effet.isBefore(actuelle.getDateDebut())))
            throw new BusinessException("La date d’effet doit être comprise entre le début de l’affectation actuelle et aujourd’hui.");
        var service = AffectationAutomatiqueService.serviceDuPoste(demande.getPosteCible());
        if (!demande.getPosteCible().isMobiliteOuverte()) throw new BusinessException("Le poste de destination n’est plus ouvert à la mobilité.");
        if (service == null) throw new BusinessException("Renseignez le service du poste de destination.");
        if (actuelle.getPoste().getId().equals(demande.getPosteCible().getId()))
            throw new BusinessException("Le poste demandé est déjà le poste actuel.");
        actuelle.setDateFin(effet); actuelle.setActuelle(false);
        affectations.saveAndFlush(actuelle);
        enregistrer(dossier, demande.getPosteCible(), service, effet, demande.getType().name(), demande.getId());
        demande.getAuteur().setService(service);
    }

    private void enregistrer(DossierEmploye dossier, com.sirh.organisation.entity.Poste poste,
            com.sirh.organisation.entity.Service service, LocalDate debut, String origine, Long source) {
        affectations.save(Affectation.builder().dossier(dossier).poste(poste).service(service)
                .posteLibelle(poste.getIntitule()).serviceLibelle(service == null ? null : service.getNom())
                .dateDebut(debut).actuelle(true).origine(origine).sourceId(source).build());
    }
}
