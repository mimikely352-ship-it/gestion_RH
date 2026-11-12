package com.sirh.presence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Table(name="regime_travail", uniqueConstraints=@UniqueConstraint(columnNames={"dossier_id","depuis"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RegimeTravail {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="dossier_id",nullable=false) private Long dossierId;
    @Column(nullable=false) private LocalDate depuis;
    @Column(nullable=false) private LocalTime debut;
    @Column(nullable=false) private LocalTime fin;
    @Column(name="pause_minutes",nullable=false) private int pauseMinutes;
    @Column(nullable=false,length=20) private String jours;
}
