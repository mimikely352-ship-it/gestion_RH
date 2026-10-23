package com.sirh.organisation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "entreprise")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Entreprise {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 150)
    private String nom;
}
