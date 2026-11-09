package com.sirh.commun.enums;

public enum StatutContrat {
    /** Contrat actuellement applicable (un seul par employé). */
    EN_COURS,
    /** Arrivé à son terme ou remplacé par un contrat suivant (CDD renouvelé, passage en CDI...). */
    TERMINE,
    /** Interrompu par un départ (démission, renvoi). */
    ROMPU
}
