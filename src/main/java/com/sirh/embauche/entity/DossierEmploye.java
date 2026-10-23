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
}
