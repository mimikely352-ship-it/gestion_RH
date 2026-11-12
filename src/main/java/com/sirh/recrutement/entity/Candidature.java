package com.sirh.recrutement.entity;

import com.sirh.candidat.entity.Candidat;
import com.sirh.candidat.entity.Document;
import com.sirh.commun.enums.StatutCandidature;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidature", uniqueConstraints = @UniqueConstraint(columnNames = {"offre_id", "candidat_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_id", nullable = false)
    private OffreEmploi offre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidat_id", nullable = false)
    private Candidat candidat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_document_id")
    private Document cvDocument;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "candidature_document",
            joinColumns = @JoinColumn(name = "candidature_id"),
            inverseJoinColumns = @JoinColumn(name = "document_id"))
    private java.util.Set<Document> piecesComplementaires = new java.util.HashSet<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "candidature_diplome",
            joinColumns = @JoinColumn(name = "candidature_id"),
            inverseJoinColumns = @JoinColumn(name = "diplome_id"))
    private java.util.Set<com.sirh.referentiel.entity.Diplome> diplomes = new java.util.HashSet<>();

    @Version
    private Long version;


    @Column(name = "score_matching", precision = 5, scale = 2)
    private BigDecimal scoreMatching;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private StatutCandidature statut = StatutCandidature.RECUE;

    @Column(name = "date_candidature", nullable = false)
    private LocalDateTime dateCandidature;

    @Column(name = "date_entree")
    private java.time.LocalDate dateEntree;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposition_statut", length = 20)
    private com.sirh.commun.enums.StatutProposition propositionStatut;
    @Enumerated(EnumType.STRING)
    @Column(name = "proposition_type_contrat", length = 25)
    private com.sirh.commun.enums.TypeContrat propositionTypeContrat;
    @Column(name = "proposition_salaire", precision = 19, scale = 2)
    private BigDecimal propositionSalaire;
    @Column(name = "proposition_debut")
    private java.time.LocalDate propositionDebut;
    @Column(name = "proposition_fin")
    private java.time.LocalDate propositionFin;
    @Column(name = "proposition_envoyee_le")
    private LocalDateTime propositionEnvoyeeLe;
    @Column(name = "proposition_repondue_le")
    private LocalDateTime propositionRepondueLe;
    /** Rôle attribué à l'embauche (EMPLOYE, CHEF_SERVICE, DIRECTION), fixé dans la proposition. */
    @Enumerated(EnumType.STRING)
    @Column(name = "proposition_role", length = 20)
    private com.sirh.commun.enums.Role propositionRole;
    /** Service d'accueil retenu dans la proposition (service du poste, ou choisi par les RH). */
    @Column(name = "proposition_service_id")
    private Long propositionServiceId;
    /** Compte RH ayant envoyé la proposition : auteur du contrat créé à l'acceptation. */
    @Column(name = "proposition_envoyee_par", length = 150)
    private String propositionEnvoyeePar;

    @PrePersist
    void prePersist() {
        if (dateCandidature == null) {
            dateCandidature = LocalDateTime.now();
        }
    }

    /** Étape de la timeline affichée côté candidat (0 = reçue, 4 = décision finale). */
    @Transient
    public int getEtapeOrdinale() {
        return switch (statut) {
            case RECUE, NON_RETENUE -> 0;
            case ACCEPTEE_ENTRETIEN, EN_ENTRETIEN, REFUSEE_ENTRETIEN -> 1;
            case EN_TEST -> 2;
            case EN_EVALUATION, REFUSEE -> 3;
            case RETENUE, EMBAUCHE -> 4;
        };
    }

    @Transient
    public boolean isRefusee() {
        return statut == StatutCandidature.NON_RETENUE
                || statut == StatutCandidature.REFUSEE_ENTRETIEN
                || statut == StatutCandidature.REFUSEE;
    }

    @Transient
    public String getCandidatNomComplet() {
        return candidat == null ? "" : candidat.getPrenom() + " " + candidat.getNom();
    }

    @Transient
    public String getOffreTitre() {
        return offre == null ? "" : offre.getTitre();
    }
}
