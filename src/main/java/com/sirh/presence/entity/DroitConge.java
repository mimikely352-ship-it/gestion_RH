package com.sirh.presence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="droit_conge",uniqueConstraints=@UniqueConstraint(columnNames={"dossier_id","type_id","annee"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DroitConge {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="dossier_id",nullable=false) private Long dossierId;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="type_id") private TypeConge type;
    @Column(nullable=false) private int annee;
    @Column(nullable=false) private int jours;
    @Column(nullable=false,length=255) private String motif;
    @Column(name="modifie_par",nullable=false,length=150) private String modifiePar;
}
