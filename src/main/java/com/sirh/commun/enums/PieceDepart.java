package com.sirh.commun.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Pièces du dossier de départ, selon le type (démission ou renvoi). */
public enum PieceDepart {
    LETTRE_DEMISSION("Lettre de démission signée par l’employé", Set.of(TypeDepart.DEMISSION), true),
    ACCUSE_RECEPTION_DEMISSION("Accusé de réception de la démission", Set.of(TypeDepart.DEMISSION), false),
    CONVOCATION_ENTRETIEN("Convocation à l’entretien préalable", Set.of(TypeDepart.LICENCIEMENT), true),
    COMPTE_RENDU_ENTRETIEN("Compte rendu de l’entretien préalable", Set.of(TypeDepart.LICENCIEMENT), false),
    LETTRE_LICENCIEMENT("Lettre de licenciement notifiée", Set.of(TypeDepart.LICENCIEMENT), true),
    CERTIFICAT_TRAVAIL("Certificat de travail", Set.of(TypeDepart.DEMISSION, TypeDepart.LICENCIEMENT), true),
    SOLDE_TOUT_COMPTE("Reçu pour solde de tout compte", Set.of(TypeDepart.DEMISSION, TypeDepart.LICENCIEMENT), true);

    private final String libelle;
    private final Set<TypeDepart> types;
    private final boolean obligatoire;

    PieceDepart(String libelle, Set<TypeDepart> types, boolean obligatoire) {
        this.libelle = libelle;
        this.types = types;
        this.obligatoire = obligatoire;
    }

    public String getLibelle() { return libelle; }
    public boolean isObligatoire() { return obligatoire; }
    public boolean concerne(TypeDepart type) { return types.contains(type); }

    public static List<PieceDepart> pour(TypeDepart type) {
        return Arrays.stream(values()).filter(p -> p.concerne(type)).toList();
    }
}
