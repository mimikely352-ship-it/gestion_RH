package com.sirh.config;

import com.sirh.commun.enums.Role;
import com.sirh.embauche.entity.Affectation;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.embauche.repository.AffectationRepository;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.organisation.entity.Service;
import com.sirh.organisation.repository.PosteRepository;
import com.sirh.organisation.repository.SectionRepository;
import com.sirh.organisation.repository.ServiceRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Crée des comptes de démonstration au démarrage (un par rôle), uniquement si app.demo-data.enabled=true
 * (à désactiver en production). Les référentiels (diplômes, organisation) viennent de sql/02_seed.sql ;
 * les comptes utilisateurs sont créés ici car le mot de passe doit être hashé par le PasswordEncoder
 * de l'application, pas en dur dans un script SQL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final ServiceRepository serviceRepository;
    private final SectionRepository sectionRepository;
    private final PosteRepository posteRepository;
    private final DossierEmployeRepository dossierEmployeRepository;
    private final AffectationRepository affectationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-data.enabled}")
    private boolean enabled;

    private static final String MOT_DE_PASSE_DEMO = "password123";

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        List<Service> services = serviceRepository.findAll();
        Service serviceDemo = services.isEmpty() ? null : services.get(0);

        creerSiAbsent("dg@sirh.local", Role.DG, null);
        creerSiAbsent("rh@sirh.local", Role.RH, null);
        Utilisateur chef = creerSiAbsent("chef-service@sirh.local", Role.CHEF_SERVICE, serviceDemo);
        creerSiAbsent("direction@sirh.local", Role.DIRECTION, serviceDemo);
        Utilisateur employe = creerSiAbsent("employe@sirh.local", Role.EMPLOYE, serviceDemo);
        if (employe != null && serviceDemo != null) {
            completerDossierSiAbsent(employe, serviceDemo);
        }

        if (serviceDemo != null && chef != null && serviceDemo.getChefService() == null) {
            serviceDemo.setChefService(chef);
            serviceRepository.save(serviceDemo);
        }

        log.info("Comptes de démonstration prêts. Désactiver app.demo-data.enabled en production.");
    }

    private Utilisateur creerSiAbsent(String email, Role role, Service service) {
        if (utilisateurRepository.existsByEmail(email)) {
            return utilisateurRepository.findByEmail(email).orElse(null);
        }
        Utilisateur utilisateur = Utilisateur.builder()
                .email(email)
                .motDePasse(passwordEncoder.encode(MOT_DE_PASSE_DEMO))
                .role(role)
                .service(service)
                .actif(true)
                .build();
        return utilisateurRepository.save(utilisateur);
    }

    /**
     * Sans dossier ni affectation, un compte Employé démo ne peut pas déposer de demande
     * de transfert/promotion ("Votre dossier employé doit être complété par les RH").
     * À la différence d'une embauche réelle, ce compte n'est jamais passé par le pipeline
     * de recrutement qui crée normalement ces enregistrements.
     */
    private void completerDossierSiAbsent(Utilisateur employe, Service service) {
        if (dossierEmployeRepository.findByUtilisateurId(employe.getId()).isPresent()) {
            return;
        }
        var sections = sectionRepository.findByServiceId(service.getId());
        if (sections.isEmpty()) {
            return;
        }
        var postes = posteRepository.findBySectionId(sections.get(0).getId());
        if (postes.isEmpty()) {
            return;
        }
        var poste = postes.get(0);
        var dateEntree = LocalDate.now();
        var dossier = dossierEmployeRepository.save(DossierEmploye.builder()
                .utilisateur(employe).nom("Démo").prenom("Employé").dateEntree(dateEntree).build());
        affectationRepository.save(Affectation.builder().dossier(dossier).poste(poste).service(service)
                .posteLibelle(poste.getIntitule()).serviceLibelle(service.getNom())
                .dateDebut(dateEntree).actuelle(true).origine("DEMO").sourceId(dossier.getId()).build());
    }
}
