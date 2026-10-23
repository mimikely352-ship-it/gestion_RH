package com.sirh.embauche.entity;

import com.sirh.organisation.entity.Poste;
import com.sirh.organisation.entity.Service;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "affectation_employe")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Affectation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false) private DossierEmploye dossier;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "poste_id", nullable = false) private Poste poste;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_id") private Service service;
    @Column(name = "poste_libelle", nullable = false, length = 150) private String posteLibelle;
    @Column(name = "service_libelle", length = 100) private String serviceLibelle;
    @Column(name = "date_debut") private LocalDate dateDebut;
    @Column(name = "date_fin") private LocalDate dateFin;
    @Column(nullable = false) private boolean actuelle;
    @Column(nullable = false, length = 20) private String origine;
    @Column(name = "source_id") private Long sourceId;
}
