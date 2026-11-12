package com.sirh.embauche.service;

import com.sirh.commun.enums.ActionAudit;
import com.sirh.embauche.entity.AuditDossier;
import com.sirh.embauche.repository.AuditDossierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Journal d'audit du dossier salarié. Écrit dans sa propre transaction pour ne jamais être perdu. */
@Service
@RequiredArgsConstructor
public class AuditDossierService {
    private final AuditDossierRepository journal;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void journaliser(Long dossierId, Authentication compte, ActionAudit action, String detail) {
        if (dossierId == null) return;
        String role = compte == null ? null : compte.getAuthorities().stream()
                .map(a -> a.getAuthority()).filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).findFirst().orElse(null);
        journal.save(AuditDossier.builder().dossierId(dossierId).horodatage(LocalDateTime.now())
                .acteur(compte == null ? "système" : compte.getName()).role(role).action(action)
                .detail(detail == null ? null : detail.length() > 500 ? detail.substring(0, 500) : detail)
                .build());
    }

    /**
     * Inscription faite dans la transaction métier en cours (embauche, bascule automatique d'un contrat) :
     * le dossier peut ne pas être encore validé en base, et l'entrée disparaît si l'opération est annulée.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void journaliserDansTransaction(Long dossierId, String acteur, String role, ActionAudit action, String detail) {
        if (dossierId == null) return;
        journal.save(AuditDossier.builder().dossierId(dossierId).horodatage(LocalDateTime.now())
                .acteur(acteur == null || acteur.isBlank() ? "système" : acteur).role(role).action(action)
                .detail(detail == null ? null : detail.length() > 500 ? detail.substring(0, 500) : detail)
                .build());
    }
}
