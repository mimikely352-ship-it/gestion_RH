package com.sirh.embauche.entity;

import com.sirh.commun.enums.ActionAudit;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Journal d'audit du dossier salarié : qui a consulté ou modifié quoi, et quand. */
@Entity @Table(name = "audit_dossier")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditDossier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "dossier_id", nullable = false) private Long dossierId;
    @Column(nullable = false) private LocalDateTime horodatage;
    @Column(nullable = false, length = 150) private String acteur;
    @Column(length = 20) private String role;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private ActionAudit action;
    @Column(length = 500) private String detail;
}
