package com.sirh.commun.enums;

public enum TypeContrat {
    CDI("CDI — durée indéterminée", false),
    CDD("CDD — durée déterminée", true),
    ESSAI("Période d’essai", true),
    STAGE("Convention de stage", true),
    APPRENTISSAGE("Contrat d’apprentissage", true),
    INTERIM("Mission d’intérim", true);

    private final String libelle;
    private final boolean dureeDeterminee;

    TypeContrat(String libelle, boolean dureeDeterminee) {
        this.libelle = libelle;
        this.dureeDeterminee = dureeDeterminee;
    }

    public String getLibelle() { return libelle; }
    /** Vrai si le contrat doit avoir une date de fin prévue. */
    public boolean isDureeDeterminee() { return dureeDeterminee; }
}
