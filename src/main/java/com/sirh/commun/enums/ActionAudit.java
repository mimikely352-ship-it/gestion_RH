package com.sirh.commun.enums;

public enum ActionAudit {
    EMBAUCHE("Embauche (dossier créé)"),
    CONSULTATION("Consultation du dossier"),
    IDENTITE_MODIFIEE("Identité / coordonnées modifiées"),
    DOCUMENT_AJOUTE("Document administratif ajouté"),
    DOCUMENT_CONSULTE("Document consulté"),
    CONTRAT_AJOUTE("Contrat ajouté"),
    CONTRAT_SIGNE("Exemplaire signé déposé"),
    CONTRAT_ACTIVE("Contrat entré en vigueur"),
    CONTRAT_ANNULE("Contrat à venir annulé"),
    SALAIRE_REVISE("Rémunération révisée"),
    ONBOARDING_MODIFIE("Checklist d’intégration modifiée"),
    DATE_ENTREE_MODIFIEE("Date d’entrée modifiée"),
    PROMOTION("Promotion / mutation"),
    DEPART_OUVERT("Dossier de départ ouvert"),
    DEPART_PIECE("Pièce de départ déposée"),
    DEPART_CLOTURE("Départ clôturé"),
    DEPART_ANNULE("Dossier de départ annulé"),
    PLANNING_MODIFIE("Planning modifié"),
    POINTAGE_MODIFIE("Pointage enregistré ou corrigé"),
    DROIT_CONGE_MODIFIE("Droits à congé modifiés"),
    CONGE_MODIFIE("Demande de congé ou décision"),
    ABSENCE_MODIFIEE("Absence enregistrée ou annulée");

    private final String libelle;

    ActionAudit(String libelle) { this.libelle = libelle; }

    public String getLibelle() { return libelle; }
}
