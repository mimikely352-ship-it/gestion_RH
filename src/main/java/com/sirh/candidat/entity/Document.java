package com.sirh.candidat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String type; // CV, AUTRE

    @Column(length = 100)
    private String libelle;

    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;

    @Column(nullable = false, length = 255)
    private String chemin;

    @Column(length = 100)
    private String mime;

    private Long taille;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidat_id")
    private Candidat candidat;

    @Column(name = "date_upload", nullable = false)
    private LocalDateTime dateUpload;

    @PrePersist
    void prePersist() {
        if (dateUpload == null) {
            dateUpload = LocalDateTime.now();
        }
    }
}
