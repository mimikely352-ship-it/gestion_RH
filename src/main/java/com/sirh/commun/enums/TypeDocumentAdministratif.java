package com.sirh.commun.enums;

/**
 * Pièces administratives du dossier salarié.
 * {@code obligatoire} : exigée pour un dossier complet.
 * {@code aExpiration} : la pièce a une date de fin de validité (à renouveler).
 * {@code validiteMois} : durée de validité usuelle, proposée quand seule la date d'émission est saisie (0 = à saisir).
 */
public enum TypeDocumentAdministratif {
    CIN("Carte d’identité nationale (CIN)", true, false, 0),
    ACTE_NAISSANCE("Copie d’acte de naissance", true, false, 0),
    CERTIFICAT_RESIDENCE("Certificat de résidence", true, true, 3),
    CASIER_JUDICIAIRE("Bulletin n°3 du casier judiciaire", true, true, 3),
    CERTIFICAT_MEDICAL("Certificat médical d’aptitude", true, true, 12),
    DIPLOME("Diplôme ou attestation de réussite", true, false, 0),
    PHOTO("Photo d’identité", false, false, 0),
    RIB("Relevé d’identité bancaire (RIB)", false, false, 0),
    PERMIS_CONDUIRE("Permis de conduire", false, true, 0),
    PASSEPORT("Passeport", false, true, 0),
    AUTRE("Autre document administratif", false, false, 0);

    private final String libelle;
    private final boolean obligatoire;
    private final boolean aExpiration;
    private final int validiteMois;

    TypeDocumentAdministratif(String libelle, boolean obligatoire, boolean aExpiration, int validiteMois) {
        this.libelle = libelle;
        this.obligatoire = obligatoire;
        this.aExpiration = aExpiration;
        this.validiteMois = validiteMois;
    }

    public String getLibelle() { return libelle; }
    public boolean isObligatoire() { return obligatoire; }
    public boolean isAExpiration() { return aExpiration; }
    public int getValiditeMois() { return validiteMois; }
    /**
     * Pièce dont plusieurs exemplaires coexistent (plusieurs diplômes, documents divers) : un nouveau dépôt
     * s'ajoute aux précédents. Pour les autres types, le dépôt le plus récent remplace le précédent (nouvelle version).
     */
    public boolean isPlusieursExemplaires() { return this == DIPLOME || this == AUTRE; }

    public static TypeDocumentAdministratif depuis(String code) {
        if (code == null) return null;
        for (var t : values()) if (t.name().equals(code)) return t;
        return null;
    }
}
