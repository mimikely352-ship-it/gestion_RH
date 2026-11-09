package com.sirh.commun.enums;

public enum MotifLicenciement {
    FAUTE_GRAVE("Faute grave"),
    FAUTE_SIMPLE("Faute simple"),
    INSUFFISANCE_PROFESSIONNELLE("Insuffisance professionnelle"),
    MOTIF_ECONOMIQUE("Motif économique"),
    INAPTITUDE("Inaptitude médicale");

    private final String libelle;

    MotifLicenciement(String libelle) { this.libelle = libelle; }

    public String getLibelle() { return libelle; }
}
