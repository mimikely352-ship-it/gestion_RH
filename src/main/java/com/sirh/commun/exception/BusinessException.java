package com.sirh.commun.exception;

/** Violation d'une règle métier (statut incompatible, action non autorisée dans le circuit...). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
