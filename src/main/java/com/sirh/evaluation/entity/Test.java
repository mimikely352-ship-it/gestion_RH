package com.sirh.evaluation.entity;

import com.sirh.recrutement.entity.OffreEmploi;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "test", uniqueConstraints = @UniqueConstraint(columnNames = {"offre_id", "niveau"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_id", nullable = false)
    private OffreEmploi offre;

    @Column(nullable = false)
    private int niveau; // 1 à 5

    @Column(nullable = false, length = 150)
    private String titre;
}
