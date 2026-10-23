package com.sirh.embauche.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Table(name = "correction_date_entree")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorrectionDateEntree {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "dossier_id", nullable = false) private Long dossierId;
    @Column(name = "ancienne_date") private LocalDate ancienneDate;
    @Column(name = "nouvelle_date", nullable = false) private LocalDate nouvelleDate;
    @Column(nullable = false, length = 150) private String auteur;
    @Column(nullable = false) private LocalDateTime horodatage;
}
