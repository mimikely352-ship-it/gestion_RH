package com.sirh.presence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Table(name="demande_conge")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DemandeConge {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="dossier_id",nullable=false) private Long dossierId;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="type_id") private TypeConge type;
    @Column(nullable=false) private LocalDate debut;
    @Column(nullable=false) private LocalDate fin;
    @Column(nullable=false) private int jours;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private StatutConge statut;
    @Column(nullable=false,length=500) private String motif;
    @Column(name="responsable_id") private Long responsableId;
    @Column(name="justificatif_chemin",length=255) private String justificatifChemin;
    @Column(name="soumise_le",nullable=false) private LocalDateTime soumiseLe;
    @Column(name="decision_manager_le") private LocalDateTime decisionManagerLe;
    @Column(name="decision_manager_par",length=150) private String decisionManagerPar;
    @Column(name="commentaire_manager",length=500) private String commentaireManager;
    @Column(name="decision_le") private LocalDateTime decisionLe;
    @Column(name="decision_par",length=150) private String decisionPar;
    @Column(name="commentaire",length=500) private String commentaire;
    @Column(name="annulee_le") private LocalDateTime annuleeLe;
    @Column(name="annulee_par",length=150) private String annuleePar;
    /** Motif saisi lors de l'annulation (par le salarié ou par les RH). */
    @Column(name="motif_annulation",length=500) private String motifAnnulation;
    @Version private Long version;
}
