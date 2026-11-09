package com.sirh.embauche.entity;

import com.sirh.commun.enums.StatutContrat;
import com.sirh.commun.enums.TypeContrat;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Un contrat de travail. Un employé en a plusieurs successifs ; un seul est EN_COURS. */
@Entity @Table(name = "contrat_employe")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contrat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "dossier_id", nullable = false) private Long dossierId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TypeContrat type;
    @Column(name = "date_debut", nullable = false) private LocalDate dateDebut;
    @Column(name = "date_fin_prevue") private LocalDate dateFinPrevue;
    @Column(name = "date_fin_effective") private LocalDate dateFinEffective;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatutContrat statut;
    @Column(name = "salaire_brut", precision = 12, scale = 2) private BigDecimal salaireBrut;
    @Column(name = "motif_fin", length = 255) private String motifFin;
    /** Renseignée quand l'exemplaire signé est déposé. */
    @Column(name = "date_signature") private LocalDate dateSignature;
    @Column(name = "cree_le", nullable = false) private LocalDateTime creeLe;
    @Column(name = "cree_par", nullable = false, length = 150) private String creePar;
    @Version private Long version;

    public boolean isSigne() { return dateSignature != null; }
}
