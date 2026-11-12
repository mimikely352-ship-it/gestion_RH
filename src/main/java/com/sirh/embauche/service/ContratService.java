package com.sirh.embauche.service;

import com.sirh.commun.enums.ActionAudit;
import com.sirh.commun.enums.StatutContrat;
import com.sirh.commun.enums.TypeContrat;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.embauche.entity.Contrat;
import com.sirh.embauche.entity.DossierEmploye;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrats successifs d'un employé (METIER.md, bloc 4) : on ne modifie jamais un ancien contrat, on en crée
 * un nouveau qui référence celui qu'il remplace. Un contrat qui commence aujourd'hui ou avant remplace
 * immédiatement le contrat en cours (TERMINE la veille) ; un contrat qui commence plus tard est À VENIR :
 * le contrat en cours reste applicable jusqu'à la veille, puis la bascule est automatique ({@link #activer}).
 */
@Service
@RequiredArgsConstructor
public class ContratService {
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Horizon de l'alerte « contrat arrivant à échéance ». */
    static final int ECHEANCE_JOURS = 30;

    private final ContratRepository contrats;
    private final DossierEmployeRepository dossiers;
    private final DocumentEmployeService documents;
    private final OnboardingService onboarding;
    private final RemunerationRepository remunerations;
    private final AuditDossierService audit;

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

        var prevu = contrats.findFirstByDossierIdAndStatut(dossierId, StatutContrat.A_VENIR).orElse(null);
        if (prevu != null)
            throw new BusinessException("Un contrat " + prevu.getType().name() + " est déjà prévu à partir du "
                    + prevu.getDateDebut().format(JOUR) + " : annulez-le avant d’en enregistrer un autre.");
        var enCours = contrats.findFirstByDossierIdAndStatut(dossierId, StatutContrat.EN_COURS).orElse(null);
        if (enCours != null && !debut.isAfter(enCours.getDateDebut()))
            throw new BusinessException("Le nouveau contrat doit commencer après le début du contrat en cours ("
                    + enCours.getDateDebut().format(JOUR) + ").");
        boolean aVenir = debut.isAfter(LocalDate.now());
        if (enCours != null && !aVenir) terminer(enCours, debut, type);

        var contrat = contrats.save(Contrat.builder().dossierId(dossierId).type(type).dateDebut(debut)
                .dateFinPrevue(finPrevue).salaireBrut(salaireBrut)
                .statut(aVenir ? StatutContrat.A_VENIR : StatutContrat.EN_COURS)
                .contratPrecedentId(enCours == null ? null : enCours.getId())
                .creeLe(LocalDateTime.now()).creePar(auteur).build());
        if (salaireBrut != null)
            remunerations.save(Remuneration.builder().dossierId(dossierId).contratId(contrat.getId()).dateEffet(debut)
                    .salaireBrut(salaireBrut).motif("Salaire du contrat " + type.name())
                    .creeLe(LocalDateTime.now()).creePar(auteur).build());
        if (exemplaireSigne != null && !exemplaireSigne.isEmpty()) signer(contrat, exemplaireSigne);
        onboarding.synchroniser(dossierId);
        return contrat;
    }

    /** Le contrat remplacé se termine la veille du suivant, ou à sa fin prévue si elle est antérieure. */
    private void terminer(Contrat enCours, LocalDate debutSuivant, TypeContrat typeSuivant) {
        var veille = debutSuivant.minusDays(1);
        enCours.setStatut(StatutContrat.TERMINE);
        enCours.setDateFinEffective(enCours.getDateFinPrevue() != null && enCours.getDateFinPrevue().isBefore(veille)
                ? enCours.getDateFinPrevue() : veille);
        enCours.setMotifFin("Remplacé par un contrat " + typeSuivant.name() + " à partir du " + debutSuivant.format(JOUR));
        contrats.saveAndFlush(enCours); // libère l'unicité « un seul contrat en cours »
    }

    /** Contrats à venir dont la date de début est atteinte (bascule quotidienne). */
    @Transactional(readOnly = true)
    public List<Long> contratsAActiver() {
        return contrats.findByStatutAndDateDebutLessThanEqual(StatutContrat.A_VENIR, LocalDate.now())
                .stream().map(Contrat::getId).toList();
    }

    /** Fait entrer en vigueur un contrat à venir arrivé à sa date de début ; le contrat en cours est terminé. */
    @Transactional
    public boolean activer(Long contratId) {
        var dossierId = contrats.dossierId(contratId).orElse(null);
        if (dossierId == null) return false;
        var dossier = dossiers.verrouillerParId(dossierId).orElse(null);
        if (dossier == null || !dossier.getUtilisateur().isActif()) return false;
        // Charger seulement après le verrou : une annulation ou un départ peut avoir eu lieu entre-temps.
        var contrat = contrats.findById(contratId).orElse(null);
        if (contrat == null || contrat.getStatut() != StatutContrat.A_VENIR || contrat.getDateDebut().isAfter(LocalDate.now()))
            return false;
        contrats.findFirstByDossierIdAndStatut(contrat.getDossierId(), StatutContrat.EN_COURS)
                .ifPresent(enCours -> terminer(enCours, contrat.getDateDebut(), contrat.getType()));
        contrat.setStatut(StatutContrat.EN_COURS);
        contrats.save(contrat);
        audit.journaliserDansTransaction(contrat.getDossierId(), "système", null, ActionAudit.CONTRAT_ACTIVE,
                contrat.getType().name() + " en vigueur depuis le " + contrat.getDateDebut().format(JOUR));
        onboarding.synchroniser(contrat.getDossierId());
        return true;
    }

    /** Annule un contrat à venir (erreur de saisie, renouvellement abandonné) : il n'a jamais pris effet. */
    @Transactional
    public Contrat annulerAVenir(Long dossierId, Long contratId, String auteur) {
        dossiers.verrouillerParId(dossierId).orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + dossierId));
        var contrat = contrats.findById(contratId)
                .filter(c -> c.getDossierId().equals(dossierId))
                .orElseThrow(() -> new ResourceNotFoundException("Contrat introuvable pour cet employé."));
        if (contrat.getStatut() != StatutContrat.A_VENIR)
            throw new BusinessException("Seul un contrat à venir peut être annulé : un contrat en cours se termine par un nouveau contrat ou un départ.");
        annuler(contrat, "Annulé par " + auteur + " le " + LocalDate.now().format(JOUR) + ", avant son début");
        onboarding.synchroniser(dossierId);
        return contrat;
    }

    /** Lors d'un départ, le contrat à venir éventuel est annulé. */
    @Transactional
    public void annulerContratAVenir(Long dossierId, String motif) {
        contrats.findFirstByDossierIdAndStatut(dossierId, StatutContrat.A_VENIR).ifPresent(c -> annuler(c, motif));
    }

    /** Les lignes de rémunération du contrat annulé n'ont jamais pris effet : elles sont retirées de l'historique. */
    private void annuler(Contrat contrat, String motif) {
        contrat.setStatut(StatutContrat.ANNULE);
        contrat.setMotifFin(motif.length() > 255 ? motif.substring(0, 255) : motif);
        contrats.save(contrat);
        var jamaisAppliquees = remunerations.findByContratIdAndDateEffetAfter(contrat.getId(), LocalDate.now());
        if (!jamaisAppliquees.isEmpty()) remunerations.deleteAll(jamaisAppliquees);
    }

    /** Contrats en cours échus ou se terminant sous 30 jours, sans contrat suivant prévu (salariés en poste). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> aEcheance() {
        var aujourdhui = LocalDate.now();
        var proches = contrats.findByStatutAndDateFinPrevueLessThanEqualOrderByDateFinPrevueAsc(
                StatutContrat.EN_COURS, aujourdhui.plusDays(ECHEANCE_JOURS));
        if (proches.isEmpty()) return List.of();
        var ids = proches.stream().map(Contrat::getDossierId).collect(Collectors.toSet());
        var renouveles = contrats.findByDossierIdInAndStatut(ids, StatutContrat.A_VENIR).stream()
                .map(Contrat::getDossierId).collect(Collectors.toSet());
        var actifs = dossiers.findAllById(ids).stream().filter(d -> d.getUtilisateur().isActif())
                .collect(Collectors.toMap(DossierEmploye::getId, d -> d));
        var resultat = new ArrayList<Map<String, Object>>();
        for (var c : proches) {
            var d = actifs.get(c.getDossierId());
            if (d == null || renouveles.contains(c.getDossierId())) continue;
            long jours = ChronoUnit.DAYS.between(aujourdhui, c.getDateFinPrevue());
            resultat.add(Map.of("dossierId", d.getId(), "nom", d.getPrenom() + " " + d.getNom(),
                    "matricule", d.getMatricule() == null ? "" : d.getMatricule(),
                    "contrat", c.getType().getLibelle(), "fin", c.getDateFinPrevue().format(JOUR), "echu", jours < 0,
                    "etat", jours < 0 ? "Échu depuis " + (-jours) + " jour(s)"
                            : jours == 0 ? "Se termine aujourd’hui" : "Dans " + jours + " jour(s)"));
        }
        return resultat;
    }

    /** Dépose l'exemplaire signé d'un contrat : la tâche d'onboarding « Signature du contrat » se coche seule. */
    @Transactional
    public void joindreExemplaireSigne(Long dossierId, Long contratId, MultipartFile fichier) {
        var contrat = contrats.findById(contratId)
                .filter(c -> c.getDossierId().equals(dossierId))
                .orElseThrow(() -> new ResourceNotFoundException("Contrat introuvable pour cet employé."));
        if (contrat.getStatut() == StatutContrat.ANNULE)
            throw new BusinessException("Ce contrat a été annulé avant son début : il ne peut pas être signé.");
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
