package com.sirh.embauche.entity;

import com.sirh.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "dossier_employe")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DossierEmploye {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true) private Utilisateur utilisateur;
    @Column(nullable = false, length = 100) private String nom;
    @Column(nullable = false, length = 100) private String prenom;
    @Column(name = "date_entree") private LocalDate dateEntree;
    @Version private Long version;

    /** Attribué par la base (trigger, format EMP-AAAA-0001) à la création du dossier, puis relu par Hibernate. */
    @org.hibernate.annotations.Generated
    @Column(length = 20, insertable = false, updatable = false) private String matricule;

    // ----- Identité -----
    @Column(length = 10) private String sexe;
    @Column(name = "date_naissance") private LocalDate dateNaissance;
    @Column(name = "lieu_naissance", length = 150) private String lieuNaissance;
    @Column(length = 80) private String nationalite;
    @Column(length = 30) private String cin;
    @Column(name = "cin_delivre_le") private LocalDate cinDelivreLe;
    @Column(name = "cin_delivre_a", length = 100) private String cinDelivreA;
    @Column(name = "situation_familiale", length = 30) private String situationFamiliale;
    @Column(name = "nombre_enfants") private Integer nombreEnfants;
    @Column(name = "numero_cnaps", length = 30) private String numeroCnaps;

    // ----- Coordonnées -----
    @Column(length = 255) private String adresse;
    @Column(length = 30) private String telephone;
    @Column(name = "email_personnel", length = 150) private String emailPersonnel;
    @Column(name = "contact_urgence_nom", length = 150) private String contactUrgenceNom;
    @Column(name = "contact_urgence_telephone", length = 30) private String contactUrgenceTelephone;

    @Column(name = "mis_a_jour_le") private java.time.LocalDateTime misAJourLe;
    @Column(name = "mis_a_jour_par", length = 150) private String misAJourPar;
}
