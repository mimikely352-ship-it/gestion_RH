package com.sirh.recrutement.service;

import com.sirh.commun.enums.TypeNotification;
import com.sirh.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Clôture automatique des offres dont la date limite est dépassée : au démarrage puis chaque nuit. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClotureOffresPlanifiee {

    private final OffreEmploiService offres;
    private final NotificationService notifications;

    @EventListener(ApplicationReadyEvent.class)
    public void auDemarrage() { executer(); }

    @Scheduled(cron = "${app.offres.cloture-cron:0 5 0 * * *}")
    public void chaqueNuit() { executer(); }

    void executer() {
        try {
            for (var offre : offres.cloturerOffresExpirees()) {
                notifications.notifierRh("L’offre « " + offre.getTitre() + " » a été clôturée automatiquement (date limite dépassée).",
                        TypeNotification.CANDIDATURE, "/rh/offres/" + offre.getId());
                log.info("Offre {} clôturée automatiquement (date limite {}).", offre.getId(), offre.getDateLimite());
            }
        } catch (RuntimeException e) {
            log.error("Échec de la clôture automatique des offres expirées.", e);
        }
    }
}
