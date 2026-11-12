package com.sirh.presence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="type_conge")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TypeConge {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=80) private String nom;
    @Column(name="deduit_solde",nullable=false) private boolean deduitSolde;
    @Column(nullable=false) private boolean remunere;
    @Column(name="justificatif_requis",nullable=false) private boolean justificatifRequis;
    @Column(nullable=false) private boolean actif;
}
