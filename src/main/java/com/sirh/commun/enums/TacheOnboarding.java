package com.sirh.commun.enums;

/**
 * Checklist d'intégration d'un nouvel employé. Les tâches automatiques sont validées par
 * les données elles-mêmes (compte de connexion actif, contrat en cours signé) ;
 * les autres sont cochées par le responsable indiqué.
 */
public enum TacheOnboarding {
    CONTRAT_SIGNE("Contrat signé", "RH", true),
    COMPTE("Compte de connexion (login) créé", "Informatique", true),
    BADGE("Badge d’accès remis", "Services généraux", false),
    MATERIEL("Matériel de travail remis", "Informatique", false),
    FORMATION("Formation d’accueil suivie", "Responsable du service", false),
    INSCRIPTION_PAIE("Inscription à la paie", "Paie", false);

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
}
