package com.sirh.presence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Table(name="jour_planning",uniqueConstraints=@UniqueConstraint(columnNames={"dossier_id","jour"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class JourPlanning {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="dossier_id",nullable=false) private Long dossierId;
    @Column(nullable=false) private LocalDate jour;
    @Column(nullable=false) private boolean repos;
    private LocalTime debut;
    private LocalTime fin;
    @Column(name="pause_minutes",nullable=false) private int pauseMinutes;
}
