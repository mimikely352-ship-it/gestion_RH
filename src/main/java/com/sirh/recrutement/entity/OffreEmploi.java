package com.sirh.recrutement.entity;

import com.sirh.commun.enums.StatutOffre;
import com.sirh.demande.entity.Demande;
import com.sirh.organisation.entity.Poste;
import com.sirh.referentiel.entity.Diplome;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "offre_emploi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OffreEmploi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private com.sirh.organisation.entity.Entreprise entreprise;

    @Transient
    public String getEntrepriseNom() {
        return entreprise == null ? "Entreprise à préciser" : entreprise.getNom();
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poste_id", nullable = false)
    private Poste poste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_origine_id")
    private Demande demandeOrigine;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "nb_postes", nullable = false)
    private int nbPostes;

    @Column(name = "experience_min_mois")
    private Integer experienceMinMois; // null = pas d'exigence

    @Column(name = "seuil_acceptation", nullable = false)
    @Builder.Default
    private int seuilAcceptation = 60;

    @Column(name = "date_publication")
    private LocalDate datePublication;

    @Column(name = "date_limite")
    private LocalDate dateLimite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutOffre statut = StatutOffre.BROUILLON;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "offre_diplome_requis",
            joinColumns = @JoinColumn(name = "offre_id"),
            inverseJoinColumns = @JoinColumn(name = "diplome_id"))
    private Set<Diplome> diplomesRequis = new HashSet<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "offre_piece_requise", joinColumns = @JoinColumn(name = "offre_id"))
    @Column(name = "libelle", nullable = false, length = 100)
    private Set<String> piecesRequises = new HashSet<>();

    @Transient
    public java.util.List<String> getListePiecesRequises() {
        return piecesRequises.stream().sorted().toList();
    }

    /** Propriété à plat pour l'affichage dans les listes (<ui:list columns="...posteIntitule...">). */
    @Transient
    public String getPosteIntitule() {
        return poste == null ? "" : poste.getIntitule();
    }
}
