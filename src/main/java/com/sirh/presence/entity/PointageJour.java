package com.sirh.presence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Table(name="pointage_jour",uniqueConstraints=@UniqueConstraint(columnNames={"dossier_id","jour"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PointageJour {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="dossier_id",nullable=false) private Long dossierId;
    @Column(nullable=false) private LocalDate jour;
    @Column(nullable=false) private LocalDateTime entree;
    private LocalDateTime sortie;
    @Column(name="pause_minutes",nullable=false) private int pauseMinutes;
    @Column(name="debut_prevu") private LocalTime debutPrevu;
    @Column(name="minutes_prevues",nullable=false) private int minutesPrevues;
    @Column(name="supplementaires_validees",nullable=false) private int supplementairesValidees;
    @Column(name="modifie_par",length=150) private String modifiePar;
    @Column(name="motif_correction",length=255) private String motifCorrection;
    @Version private Long version;
}
