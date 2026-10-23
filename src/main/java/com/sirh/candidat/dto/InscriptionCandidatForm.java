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

    @jakarta.validation.constraints.NotBlank(message="Renseignez votre email.")
    @jakarta.validation.constraints.Email(message="L’email n’est pas valide.")
    @jakarta.validation.constraints.Size(max=150)
    private String email;
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(min=10,max=72,message="Le mot de passe doit contenir 10 à 72 caractères.")
    private String motDePasse;
    @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=100)
    private String nom;
    @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=100)
    private String prenom;
    @jakarta.validation.constraints.Size(max=20)
    private String telephone;
    @jakarta.validation.constraints.Past
    private LocalDate dateNaissance;
    @jakarta.validation.constraints.Size(max=150)
    private String lieuNaissance;
    @jakarta.validation.constraints.Size(max=30)
    private String cin;
    @jakarta.validation.constraints.Size(max=255)
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
