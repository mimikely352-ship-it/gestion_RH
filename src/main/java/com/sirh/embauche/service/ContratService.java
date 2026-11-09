package com.sirh.embauche.service;

import com.sirh.commun.enums.StatutContrat;
import com.sirh.commun.enums.TypeContrat;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.embauche.entity.Contrat;
import com.sirh.embauche.entity.Remuneration;
import com.sirh.embauche.repository.ContratRepository;
import com.sirh.embauche.repository.RemunerationRepository;
import com.sirh.embauche.repository.DossierEmployeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Contrats successifs d'un employé. Ajouter un contrat clôt le contrat en cours (TERMINE)
 * la veille du nouveau : l'historique (CDD puis CDI, renouvellements...) est conservé.
 */
@Service
@RequiredArgsConstructor
public class ContratService {
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContratRepository contrats;
    private final DossierEmployeRepository dossiers;
    private final DocumentEmployeService documents;
    private final OnboardingService onboarding;
    private final RemunerationRepository remunerations;

    @Transactional
    public Contrat ajouter(Long dossierId, TypeContrat type, LocalDate debut, LocalDate finPrevue,
                           BigDecimal salaireBrut, MultipartFile exemplaireSigne, String auteur) {
        var dossier = dossiers.verrouillerParId(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + dossierId));
        if (!dossier.getUtilisateur().isActif())
            throw new BusinessException("Cet employé a quitté l’entreprise : aucun nouveau contrat ne peut être ajouté.");
        if (type == null) throw new BusinessException("Choisissez le type de contrat.");
        if (debut == null) throw new BusinessException("Indiquez la date de début du contrat.");
        if (type.isDureeDeterminee()) {
            if (finPrevue == null) throw new BusinessException("Un contrat « " + type.getLibelle() + " » doit avoir une date de fin prévue.");
            if (!finPrevue.isAfter(debut)) throw new BusinessException("La date de fin prévue doit être après la date de début.");
        } else {
            finPrevue = null;
        }
        if (salaireBrut != null && salaireBrut.signum() < 0) throw new BusinessException("Le salaire ne peut pas être négatif.");

        var enCours = contrats.findFirstByDossierIdAndStatut(dossierId, StatutContrat.EN_COURS).orElse(null);
        if (enCours != null) {
            if (!debut.isAfter(enCours.getDateDebut()))
                throw new BusinessException("Le nouveau contrat doit commencer après le début du contrat en cours ("
                        + enCours.getDateDebut().format(JOUR) + ").");
            enCours.setStatut(StatutContrat.TERMINE);
            enCours.setDateFinEffective(debut.minusDays(1));
            enCours.setMotifFin("Remplacé par un contrat " + type.name() + " à partir du " + debut.format(JOUR));
            contrats.saveAndFlush(enCours); // libère l'unicité « un seul contrat en cours »
        }
        var contrat = contrats.save(Contrat.builder().dossierId(dossierId).type(type).dateDebut(debut)
                .dateFinPrevue(finPrevue).salaireBrut(salaireBrut).statut(StatutContrat.EN_COURS)
                .creeLe(LocalDateTime.now()).creePar(auteur).build());
        if (salaireBrut != null)
            remunerations.save(Remuneration.builder().dossierId(dossierId).contratId(contrat.getId()).dateEffet(debut)
                    .salaireBrut(salaireBrut).motif("Salaire du contrat " + type.name())
                    .creeLe(LocalDateTime.now()).creePar(auteur).build());
        if (exemplaireSigne != null && !exemplaireSigne.isEmpty()) signer(contrat, exemplaireSigne);
        onboarding.synchroniser(dossierId);
        return contrat;
    }

    /** Dépose l'exemplaire signé d'un contrat : la tâche d'onboarding « Contrat signé » se coche seule. */
    @Transactional
    public void joindreExemplaireSigne(Long dossierId, Long contratId, MultipartFile fichier) {
        var contrat = contrats.findById(contratId)
                .filter(c -> c.getDossierId().equals(dossierId))
                .orElseThrow(() -> new ResourceNotFoundException("Contrat introuvable pour cet employé."));
        signer(contrat, fichier);
        onboarding.synchroniser(dossierId);
    }

    private void signer(Contrat contrat, MultipartFile fichier) {
        documents.deposer(fichier, "CONTRAT", "Contrat " + contrat.getType().name() + " signé",
                contrat.getDossierId(), contrat.getId(), null);
        contrat.setDateSignature(LocalDate.now());
        contrats.save(contrat);
    }
}
