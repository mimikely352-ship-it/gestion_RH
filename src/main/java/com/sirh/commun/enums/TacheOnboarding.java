package com.sirh.commun.enums;

/**
 * Checklist d'intégration d'un nouvel employé (METIER.md, bloc 4). Les tâches automatiques sont
 * validées par les données elles-mêmes (contrat d'embauche signé, compte de connexion actif) ;
 * les autres sont cochées par leur responsable : RH, ou responsable du service (« Manager »).
 */
public enum TacheOnboarding {
    CONTRAT_SIGNE("Signature du contrat", "RH", true),
    BADGE("Création du badge", "RH", false),
    COMPTE("Création du compte informatique (login)", "Admin", true),
    MATERIEL("Remise du matériel", "Manager", false),
    FORMATION("Formation d’intégration", "Manager", false),
    INSCRIPTION_PAIE("Inscription à la paie", "RH", false),
    COMPTE_BANCAIRE("Ouverture du compte bancaire", "RH", false);

    private final String libelle;
    private final String responsable;
    private final boolean automatique;

    TacheOnboarding(String libelle, String responsable, boolean automatique) {
        this.libelle = libelle;
        this.responsable = responsable;
        this.automatique = automatique;
    }

    public String getLibelle() { return libelle; }
    public String getResponsable() { return responsable; }
    public boolean isAutomatique() { return automatique; }
    /** Tâche confiée au responsable du service : le chef de service peut la cocher. */
    public boolean isManager() { return "Manager".equals(responsable); }
}
