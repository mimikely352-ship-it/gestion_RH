package com.sirh.embauche.entity;

import com.sirh.commun.enums.StatutDepart;
import com.sirh.commun.enums.TypeDepart;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Dossier de sortie d'un employé : démission ou renvoi (licenciement), avec ses pièces. */
@Entity @Table(name = "depart_employe")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Depart {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false) private DossierEmploye dossier;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TypeDepart type;
    /** Pour un renvoi : code de MotifLicenciement. */
    @Column(length = 40) private String motif;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "date_notification", nullable = false) private LocalDate dateNotification;
    /** Dernier jour dans l'entreprise. */
    @Column(name = "date_effet", nullable = false) private LocalDate dateEffet;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatutDepart statut;
    @Column(name = "cree_le", nullable = false) private LocalDateTime creeLe;
    @Column(name = "cree_par", nullable = false, length = 150) private String creePar;
    @Column(name = "cloture_le") private LocalDateTime clotureLe;
    @Column(name = "cloture_par", length = 150) private String cloturePar;
    @Version private Long version;
}
