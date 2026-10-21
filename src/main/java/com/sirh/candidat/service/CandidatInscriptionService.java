package com.sirh.candidat.service;

import com.sirh.candidat.entity.Candidat;
import com.sirh.candidat.entity.Document;
import com.sirh.candidat.entity.Experience;
import com.sirh.candidat.repository.CandidatRepository;
import com.sirh.candidat.repository.DocumentRepository;
import com.sirh.candidat.repository.ExperienceRepository;
import com.sirh.commun.enums.Role;
import com.sirh.commun.exception.BusinessException;
import com.sirh.referentiel.entity.Diplome;
import com.sirh.referentiel.repository.DiplomeRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Crée le compte et le profil candidat (informations personnelles, CV, expériences, diplômes) en
 * une seule opération, conformément au formulaire de candidature du cahier des charges (§4.1.1).
 * Ce profil est ensuite réutilisé pour chaque candidature ultérieure du même candidat.
 */
@Service
@RequiredArgsConstructor
public class CandidatInscriptionService {

    private final UtilisateurRepository utilisateurRepository;
    private final CandidatRepository candidatRepository;
    private final ExperienceRepository experienceRepository;
    private final DocumentRepository documentRepository;
    private final DiplomeRepository diplomeRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public record ExperienceSaisie(String intitule, String entreprise, LocalDate dateDebut, LocalDate dateFin, String description) {
    }

    @Transactional
    public Candidat inscrire(String email, String motDePasse, String nom, String prenom, String telephone,
                              LocalDate dateNaissance, String lieuNaissance, String cin, String adresse,
                              List<ExperienceSaisie> experiences, Set<Long> diplomeIds, MultipartFile cv) {
        if (utilisateurRepository.existsByEmail(email)) {
            throw new BusinessException("Un compte existe déjà avec cet email.");
        }

        Utilisateur utilisateur = Utilisateur.builder()
                .email(email)
                .motDePasse(passwordEncoder.encode(motDePasse))
                .role(Role.CANDIDAT)
                .actif(true)
                .build();
        utilisateur = utilisateurRepository.save(utilisateur);

        Set<Diplome> diplomes = diplomeIds == null || diplomeIds.isEmpty()
                ? new HashSet<>()
                : new HashSet<>(diplomeRepository.findAllById(diplomeIds));

        Candidat candidat = Candidat.builder()
                .utilisateur(utilisateur)
                .nom(nom)
                .prenom(prenom)
                .telephone(telephone)
                .dateNaissance(dateNaissance)
                .lieuNaissance(lieuNaissance)
                .cin(cin)
                .adresse(adresse)
                .diplomes(diplomes)
                .build();
        candidat = candidatRepository.save(candidat);

        if (experiences != null) {
            for (ExperienceSaisie saisie : experiences) {
                Experience experience = Experience.builder()
                        .candidat(candidat)
                        .intitule(saisie.intitule())
                        .entreprise(saisie.entreprise())
                        .dateDebut(saisie.dateDebut())
                        .dateFin(saisie.dateFin())
                        .description(saisie.description())
                        .build();
                experienceRepository.save(experience);
            }
        }

        if (cv != null && !cv.isEmpty()) {
            String chemin = fileStorageService.enregistrer(cv);
            Document document = Document.builder()
                    .type("CV")
                    .nomFichier(cv.getOriginalFilename())
                    .chemin(chemin)
                    .mime(cv.getContentType())
                    .taille(cv.getSize())
                    .candidat(candidat)
                    .build();
            documentRepository.save(document);
        }

        return candidat;
    }
}
