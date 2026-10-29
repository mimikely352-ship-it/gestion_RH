package com.sirh.embauche.service;

import com.sirh.commun.enums.Role;
import com.sirh.embauche.entity.*;
import com.sirh.embauche.repository.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.organisation.entity.Poste;
import com.sirh.organisation.repository.PosteRepository;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.demande.entity.Demande;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service @RequiredArgsConstructor
public class ParcoursEmployeService {
    private final DossierEmployeRepository dossiers;
    private final AffectationRepository affectations;
    private final UtilisateurRepository utilisateurs;
    private final PosteRepository postes;
    private final PasswordEncoder passwordEncoder;

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

    /** Création d'un compte Employé par le RH, sans passer par une candidature/embauche recrutement. */
    @Transactional
    public Utilisateur embaucherDirectement(String email, String motDePasse, String nom, String prenom,
            Long posteId, LocalDate dateEntree) {
        if (email == null || email.isBlank()) throw new BusinessException("L’email est requis.");
        if (utilisateurs.existsByEmail(email.trim())) throw new BusinessException("Un compte existe déjà avec cet email.");
        if (motDePasse == null || motDePasse.length() < 8) throw new BusinessException("Le mot de passe doit contenir au moins 8 caractères.");
        if (nom == null || nom.isBlank() || prenom == null || prenom.isBlank()) throw new BusinessException("Le nom et le prénom sont requis.");
        Poste poste = postes.findById(posteId).orElseThrow(() -> new BusinessException("Poste introuvable."));
        var service = AffectationAutomatiqueService.serviceDuPoste(poste);
        if (service == null) throw new BusinessException("Ce poste n’a pas de service rattaché (hiérarchie incomplète).");
        LocalDate entree = dateEntree == null ? LocalDate.now() : dateEntree;
        if (entree.isAfter(LocalDate.now())) throw new BusinessException("La date d’entrée ne peut pas être future.");

        Utilisateur utilisateur = utilisateurs.save(Utilisateur.builder()
                .email(email.trim()).motDePasse(passwordEncoder.encode(motDePasse)).role(Role.EMPLOYE)
                .service(service).actif(true).build());
        var dossier = dossiers.save(DossierEmploye.builder().utilisateur(utilisateur)
                .nom(nom.trim()).prenom(prenom.trim()).dateEntree(entree).build());
        enregistrer(dossier, poste, service, entree, "EMBAUCHE_DIRECTE", null);
        return utilisateur;
    }

    /** Promotion/mobilité décidée directement par le RH, sans demande de l'employé ni circuit de validation. */
    @Transactional
    public void promouvoirDirectement(Long dossierId, Long posteCibleId, LocalDate effet) {
        var dossier = dossiers.verrouillerParId(dossierId).orElseThrow(() -> new BusinessException("Dossier employé introuvable."));
        var actuelle = affectations.findByDossierIdAndActuelleTrue(dossierId)
                .orElseThrow(() -> new BusinessException("L’affectation actuelle doit être renseignée avant ce mouvement."));
        Poste posteCible = postes.findById(posteCibleId).orElseThrow(() -> new BusinessException("Poste introuvable."));
        if (actuelle.getPoste().getId().equals(posteCible.getId()))
            throw new BusinessException("Cet employé occupe déjà ce poste.");
        var service = AffectationAutomatiqueService.serviceDuPoste(posteCible);
        if (service == null) throw new BusinessException("Ce poste n’a pas de service rattaché (hiérarchie incomplète).");
        LocalDate dateEffet = effet == null ? LocalDate.now() : effet;
        if (dateEffet.isAfter(LocalDate.now()) || (actuelle.getDateDebut() != null && dateEffet.isBefore(actuelle.getDateDebut())))
            throw new BusinessException("La date d’effet doit être comprise entre le début de l’affectation actuelle et aujourd’hui.");

        actuelle.setDateFin(dateEffet);
        actuelle.setActuelle(false);
        affectations.saveAndFlush(actuelle);
        enregistrer(dossier, posteCible, service, dateEffet, "PROMOTION_RH", null);
        dossier.getUtilisateur().setService(service);
        utilisateurs.save(dossier.getUtilisateur());
    }
}
