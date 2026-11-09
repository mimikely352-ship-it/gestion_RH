package com.sirh.commun.enums;

import java.time.LocalDate;

public enum ValiditeDocument {
    PERMANENT("Sans échéance"),
    VALIDE("Valide"),
    A_RENOUVELER("À renouveler"),
    EXPIRE("Expiré");

    /** Un document expirant dans ce délai est signalé « à renouveler ». */
    public static final int PREAVIS_JOURS = 30;

    private final String libelle;

    ValiditeDocument(String libelle) { this.libelle = libelle; }

    public String getLibelle() { return libelle; }

    public static ValiditeDocument de(LocalDate expiration, LocalDate aujourdhui) {
        if (expiration == null) return PERMANENT;
        if (expiration.isBefore(aujourdhui)) return EXPIRE;
        if (!expiration.isAfter(aujourdhui.plusDays(PREAVIS_JOURS))) return A_RENOUVELER;
        return VALIDE;
    }
}
