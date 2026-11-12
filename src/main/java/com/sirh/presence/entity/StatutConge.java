package com.sirh.presence.entity;
public enum StatutConge {
    ATTENTE_MANAGER, ATTENTE_RH, ACCEPTE, REFUSE, ANNULE;
    public boolean reserve() { return this == ATTENTE_MANAGER || this == ATTENTE_RH || this == ACCEPTE; }
}
