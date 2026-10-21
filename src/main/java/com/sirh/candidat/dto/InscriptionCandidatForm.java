package com.sirh.candidat.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Command bean du formulaire d'inscription candidat (JavaBean classique : le binding Spring
 * d'une liste indexée comme experiences[0].intitule exige des setters, pas un record). */
@Getter
@Setter
public class InscriptionCandidatForm {

    private String email;
    private String motDePasse;
    private String nom;
    private String prenom;
    private String telephone;
    private LocalDate dateNaissance;
    private String lieuNaissance;
    private String cin;
    private String adresse;
    private List<ExperienceForm> experiences = new ArrayList<>();
    private Set<Long> diplomeIds;
    private MultipartFile cv;

    @Getter
    @Setter
    public static class ExperienceForm {
        private String intitule;
        private String entreprise;
        private LocalDate dateDebut;
        private LocalDate dateFin;
        private String description;
    }
}
