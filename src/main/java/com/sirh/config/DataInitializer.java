package com.sirh.config;

import com.sirh.commun.enums.Role;
import com.sirh.organisation.entity.Service;
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
        creerSiAbsent("employe@sirh.local", Role.EMPLOYE, serviceDemo);

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
}
