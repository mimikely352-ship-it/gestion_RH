package com.sirh.commun.enums;

public enum StatutContrat {
    /** Saisi à l'avance (renouvellement, embauche avec entrée future) : devient EN_COURS à sa date de début. */
    A_VENIR,
    /** Contrat actuellement applicable (un seul par employé). */
    EN_COURS,
    /** Arrivé à son terme ou remplacé par un contrat suivant (CDD renouvelé, passage en CDI...). */
    TERMINE,
    /** Interrompu par un départ (démission, renvoi). */
    ROMPU,
    /** Contrat à venir abandonné avant son début (erreur de saisie, départ) : il n'a jamais pris effet. */
    ANNULE
}
