package com.sirh.embauche.service;

import com.sirh.commun.enums.StatutContrat;
import com.sirh.commun.enums.StatutOnboarding;
import com.sirh.commun.enums.StatutTache;
import com.sirh.commun.enums.TacheOnboarding;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.embauche.entity.Contrat;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.embauche.entity.Onboarding;
import com.sirh.embauche.entity.OnboardingTache;
import com.sirh.embauche.repository.ContratRepository;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.embauche.repository.OnboardingRepository;
import com.sirh.embauche.repository.OnboardingTacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;

/**
 * Workflow d'intégration (METIER.md, bloc 4). L'onboarding est créé avec le premier contrat, avec une tâche
 * par élément de la checklist ; il passe à TERMINE quand toutes les tâches sont faites. « Signature du contrat »
 * porte sur le contrat d'embauche (premier contrat du dossier). Une intégration terminée n'est pas rouverte
 * par un recalcul automatique (renouvellement, nouveau contrat) : seule une tâche décochée explicitement la rouvre.
 * Les tâches confiées au « Manager » peuvent être cochées par le chef du service de l'employé.
 */
@Service
@RequiredArgsConstructor
public class OnboardingService {
    static final String AUTOMATIQUE = "Automatique";
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final OnboardingRepository onboardings;
    private final OnboardingTacheRepository taches;
    private final ContratRepository contrats;
    private final DossierEmployeRepository dossiers;

    public record Etape(TacheOnboarding tache, boolean fait, String detail) {
        public String getCode() { return tache.name(); }
        public String getLibelle() { return tache.getLibelle(); }
        public String getResponsable() { return tache.getResponsable(); }
        public boolean isAutomatique() { return tache.isAutomatique(); }
        public boolean isManager() { return tache.isManager(); }
        public boolean isFait() { return fait; }
        public String getDetail() { return detail; }
    }

    public record Vue(StatutOnboarding statut, List<Etape> etapes, String termineLe) {
        public StatutOnboarding getStatut() { return statut; }
        public List<Etape> getEtapes() { return etapes; }
        public String getTermineLe() { return termineLe; }
        public int getFaits() { return (int) etapes.stream().filter(Etape::fait).count(); }
        public int getTotal() { return etapes.size(); }
        public int getPourcentage() { return etapes.isEmpty() ? 0 : getFaits() * 100 / getTotal(); }
        public boolean isTermine() { return statut == StatutOnboarding.TERMINE; }
    }

    @Transactional(readOnly = true)
    public Vue vue(DossierEmploye dossier) {
        var onboarding = onboardings.findByDossierId(dossier.getId()).orElse(null);
        var lignes = onboarding == null ? List.<OnboardingTache>of() : taches.findByOnboardingId(onboarding.getId());
        return calculer(dossier, contratEmbauche(dossier.getId()), onboarding, lignes);
    }

    /** Avancement « faits / total » pour la liste des employés. */
    @Transactional(readOnly = true)
    public Map<Long, String> avancement(Collection<Long> dossierIds) {
        if (dossierIds.isEmpty()) return Map.of();
        var ids = List.copyOf(dossierIds);
        var parDossier = new HashMap<Long, Onboarding>();
        onboardings.findByDossierIdIn(ids).forEach(o -> parDossier.put(o.getDossierId(), o));
        var tachesParOnboarding = new HashMap<Long, List<OnboardingTache>>();
        if (!parDossier.isEmpty())
            taches.findByOnboardingIdIn(parDossier.values().stream().map(Onboarding::getId).toList())
                    .forEach(t -> tachesParOnboarding.computeIfAbsent(t.getOnboarding().getId(), k -> new ArrayList<>()).add(t));
        var contratsEmbauche = new HashMap<Long, Contrat>();
        contrats.findByDossierIdInAndStatutNot(ids, StatutContrat.ANNULE)
                .forEach(c -> contratsEmbauche.merge(c.getDossierId(), c, OnboardingService::plusAncien));
        var resultat = new HashMap<Long, String>();
        for (var dossier : dossiers.findAllById(ids)) {
            var onboarding = parDossier.get(dossier.getId());
            var vue = calculer(dossier, contratsEmbauche.get(dossier.getId()), onboarding,
                    onboarding == null ? List.of() : tachesParOnboarding.getOrDefault(onboarding.getId(), List.of()));
            resultat.put(dossier.getId(), vue.getFaits() + " / " + vue.getTotal());
        }
        return resultat;
    }

    /** Enregistre la checklist cochée par les RH (toutes les tâches manuelles ; les automatiques sont recalculées). */
    @Transactional
    public void enregistrer(Long dossierId, Set<TacheOnboarding> faites, String auteur) {
        appliquer(verrouillerActif(dossierId), faites == null ? Set.of() : faites, auteur, code -> true);
    }

    /** Le chef du service coche les tâches qui lui sont confiées (matériel, formation) ; les autres restent inchangées. */
    @Transactional
    public void enregistrerParManager(Long dossierId, Set<TacheOnboarding> faites, String auteur) {
        appliquer(verrouillerActif(dossierId), faites == null ? Set.of() : faites, auteur, TacheOnboarding::isManager);
    }

    /** Recalcule les tâches automatiques (après un contrat ou une signature) ; crée l'onboarding s'il n'existe pas. */
    @Transactional
    public void synchroniser(Long dossierId) {
        dossiers.findById(dossierId).ifPresent(d -> appliquer(d, null, null, code -> false));
    }

    private DossierEmploye verrouillerActif(Long dossierId) {
        var dossier = dossiers.verrouillerParId(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + dossierId));
        if (!dossier.getUtilisateur().isActif())
            throw new BusinessException("Cet employé a quitté l’entreprise : son intégration ne peut plus être modifiée.");
        return dossier;
    }

    /** Contrat d'embauche : premier contrat du dossier, hors contrat annulé. */
    private Contrat contratEmbauche(Long dossierId) {
        return contrats.findFirstByDossierIdAndStatutNotOrderByDateDebutAscIdAsc(dossierId, StatutContrat.ANNULE).orElse(null);
    }

    static Contrat plusAncien(Contrat a, Contrat b) {
        int ordre = a.getDateDebut().compareTo(b.getDateDebut());
        if (ordre == 0) ordre = Long.compare(a.getId() == null ? 0 : a.getId(), b.getId() == null ? 0 : b.getId());
        return ordre <= 0 ? a : b;
    }

    /**
     * @param faites     null = recalcul automatique : aucune tâche manuelle n'est modifiée
     * @param modifiable tâches manuelles que l'auteur peut changer
     */
    private void appliquer(DossierEmploye dossier, Set<TacheOnboarding> faites, String auteur,
                           Predicate<TacheOnboarding> modifiable) {
        var existant = onboardings.findByDossierId(dossier.getId()).orElse(null);
        if (existant != null && faites == null && existant.getStatut() == StatutOnboarding.TERMINE) return;
        var onboarding = existant != null ? existant : onboardings.save(Onboarding.builder()
                .dossierId(dossier.getId()).statut(StatutOnboarding.EN_COURS)
                .dateDebut(dossier.getDateEntree() == null ? LocalDate.now() : dossier.getDateEntree()).build());
        var existantes = new EnumMap<TacheOnboarding, OnboardingTache>(TacheOnboarding.class);
        taches.findByOnboardingId(onboarding.getId()).forEach(t -> existantes.put(t.getCode(), t));
        var contrat = contratEmbauche(dossier.getId());
        var maintenant = LocalDateTime.now();
        boolean toutFait = true;
        for (var code : TacheOnboarding.values()) {
            var tache = existantes.get(code);
            if (tache == null) tache = OnboardingTache.builder().onboarding(onboarding).code(code).statut(StatutTache.A_FAIRE).build();
            boolean fait;
            String par;
            if (code.isAutomatique()) {
                fait = automatiqueFaite(code, dossier, contrat);
                par = AUTOMATIQUE;
            } else if (faites != null && modifiable.test(code)) {
                fait = faites.contains(code);
                par = auteur;
            } else {
                fait = tache.getStatut() == StatutTache.FAIT;
                par = tache.getFaitPar();
            }
            if (fait && tache.getStatut() != StatutTache.FAIT) {
                tache.setStatut(StatutTache.FAIT); tache.setFaitLe(maintenant); tache.setFaitPar(par);
            } else if (!fait) {
                tache.setStatut(StatutTache.A_FAIRE); tache.setFaitLe(null); tache.setFaitPar(null);
            }
            taches.save(tache);
            toutFait &= fait;
        }
        if (toutFait && onboarding.getStatut() != StatutOnboarding.TERMINE) {
            onboarding.setStatut(StatutOnboarding.TERMINE); onboarding.setTermineLe(maintenant);
        } else if (!toutFait) {
            onboarding.setStatut(StatutOnboarding.EN_COURS); onboarding.setTermineLe(null);
        }
        onboardings.save(onboarding);
    }

    static boolean automatiqueFaite(TacheOnboarding code, DossierEmploye dossier, Contrat contratEmbauche) {
        return switch (code) {
            case COMPTE -> dossier.getUtilisateur() != null && dossier.getUtilisateur().isActif();
            case CONTRAT_SIGNE -> contratEmbauche != null && contratEmbauche.isSigne();
            default -> false;
        };
    }

    /** @param contratEmbauche premier contrat du dossier (celui dont la signature fait partie de l'intégration) */
    static Vue calculer(DossierEmploye dossier, Contrat contratEmbauche, Onboarding onboarding, List<OnboardingTache> lignes) {
        var parCode = new EnumMap<TacheOnboarding, OnboardingTache>(TacheOnboarding.class);
        lignes.forEach(t -> parCode.put(t.getCode(), t));
        // Intégration close : l'état enregistré fait foi (le compte peut être désactivé plus tard, un contrat renouvelé...).
        boolean close = onboarding != null && onboarding.getStatut() == StatutOnboarding.TERMINE;
        var etapes = new ArrayList<Etape>();
        for (var code : TacheOnboarding.values()) {
            var t = parCode.get(code);
            if (close) {
                boolean fait = t == null || t.getStatut() == StatutTache.FAIT;
                etapes.add(new Etape(code, fait, t == null ? "Tâche ajoutée après la fin de l’intégration" : suivi(t)));
            } else if (code.isAutomatique()) {
                boolean fait = automatiqueFaite(code, dossier, contratEmbauche);
                String detail = switch (code) {
                    case COMPTE -> fait ? "Identifiant : " + dossier.getUtilisateur().getEmail() : "Compte absent ou désactivé";
                    case CONTRAT_SIGNE -> contratEmbauche == null ? "Aucun contrat enregistré : ajoutez-le dans la rubrique Contrats"
                            : fait ? contratEmbauche.getType().name() + " signé le " + contratEmbauche.getDateSignature().format(JOUR)
                            : contratEmbauche.getType().name() + " d’embauche non signé : déposez l’exemplaire signé";
                    default -> null;
                };
                etapes.add(new Etape(code, fait, detail));
            } else {
                boolean fait = t != null && t.getStatut() == StatutTache.FAIT;
                etapes.add(new Etape(code, fait, fait ? suivi(t) : null));
            }
        }
        boolean toutFait = etapes.stream().allMatch(Etape::fait);
        String termineLe = (close || toutFait) && onboarding != null && onboarding.getTermineLe() != null
                ? onboarding.getTermineLe().format(HORODATAGE) : null;
        return new Vue(close || toutFait ? StatutOnboarding.TERMINE : StatutOnboarding.EN_COURS, etapes, termineLe);
    }

    private static String suivi(OnboardingTache t) {
        return t.getFaitLe() == null ? null
                : "Fait le " + t.getFaitLe().format(HORODATAGE) + (t.getFaitPar() == null ? "" : " par " + t.getFaitPar());
    }
}
