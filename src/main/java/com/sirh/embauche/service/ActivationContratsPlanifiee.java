package com.sirh.embauche.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Entrée en vigueur des contrats à venir (renouvellement saisi à l'avance, embauche avec entrée future) :
 * au démarrage puis chaque nuit. Chaque contrat est traité dans sa propre transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivationContratsPlanifiee {

    private final ContratService contrats;

    @EventListener(ApplicationReadyEvent.class)
    public void auDemarrage() { executer(); }

    @Scheduled(cron = "${app.contrats.activation-cron:0 10 0 * * *}")
    public void chaqueNuit() { executer(); }

    void executer() {
        List<Long> ids;
        try {
            ids = contrats.contratsAActiver();
        } catch (RuntimeException e) {
            log.error("Recherche des contrats à venir impossible.", e);
            return;
        }
        for (var id : ids) {
            try {
                if (contrats.activer(id)) log.info("Contrat {} entré en vigueur.", id);
            } catch (RuntimeException e) {
                log.error("Échec de l’entrée en vigueur du contrat {}.", id, e);
            }
        }
    }
}
