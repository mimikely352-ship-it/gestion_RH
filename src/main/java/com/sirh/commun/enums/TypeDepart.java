package com.sirh.commun.enums;

public enum TypeDepart {
    DEMISSION("Démission"),
    LICENCIEMENT("Renvoi (licenciement)");

    private final String libelle;

    TypeDepart(String libelle) { this.libelle = libelle; }

    public String getLibelle() { return libelle; }
}
