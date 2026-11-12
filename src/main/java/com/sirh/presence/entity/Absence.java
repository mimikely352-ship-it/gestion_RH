package com.sirh.presence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Table(name="absence_employe")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Absence {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="dossier_id",nullable=false) private Long dossierId;
    @Column(name="demande_id",unique=true) private Long demandeId;
    @Column(nullable=false) private LocalDate debut;
    @Column(nullable=false) private LocalDate fin;
    @Column(nullable=false,length=120) private String libelle;
    @Column(nullable=false) private boolean remuneree;
    @Column(nullable=false) private boolean active;
    @Column(name="justificatif_chemin",length=255) private String justificatifChemin;
    @Column(name="enregistree_par",nullable=false,length=150) private String enregistreePar;
}
