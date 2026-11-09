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

/**
 * Workflow d'intégration. L'onboarding est créé au premier enregistrement avec une tâche par
 * élément de la checklist ; il passe à TERMINE quand toutes les tâches sont faites et revient
 * à EN_COURS si l'une d'elles est décochée (ou si une tâche automatique n'est plus remplie).
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
        var contrat = contrats.findFirstByDossierIdAndStatut(dossier.getId(), StatutContrat.EN_COURS).orElse(null);
        return calculer(dossier, contrat, onboarding, lignes);
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
        var contratsEnCours = new HashMap<Long, Contrat>();
        contrats.findByDossierIdInAndStatut(ids, StatutContrat.EN_COURS).forEach(c -> contratsEnCours.put(c.getDossierId(), c));
        var resultat = new HashMap<Long, String>();
        for (var dossier : dossiers.findAllById(ids)) {
            var onboarding = parDossier.get(dossier.getId());
            var vue = calculer(dossier, contratsEnCours.get(dossier.getId()), onboarding,
                    onboarding == null ? List.of() : tachesParOnboarding.getOrDefault(onboarding.getId(), List.of()));
            resultat.put(dossier.getId(), vue.getFaits() + " / " + vue.getTotal());
        }
        return resultat;
    }

    /** Enregistre la checklist cochée par les RH (les tâches automatiques sont recalculées). */
    @Transactional
    public void enregistrer(Long dossierId, Set<TacheOnboarding> faites, String auteur) {
        var dossier = dossiers.verrouillerParId(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + dossierId));
        if (!dossier.getUtilisateur().isActif())
            throw new BusinessException("Cet employé a quitté l’entreprise : son intégration ne peut plus être modifiée.");
        appliquer(dossier, faites == null ? Set.of() : faites, auteur);
    }

    /** Recalcule les tâches automatiques (appelé après un changement de contrat, par exemple). */
    @Transactional
    public void synchroniser(Long dossierId) {
        dossiers.findById(dossierId).ifPresent(d -> appliquer(d, null, null));
    }

    /** @param faites null = ne pas toucher aux tâches manuelles */
    private void appliquer(DossierEmploye dossier, Set<TacheOnboarding> faites, String auteur) {
        var onboarding = onboardings.findByDossierId(dossier.getId()).orElseGet(() -> onboardings.save(Onboarding.builder()
                .dossierId(dossier.getId()).statut(StatutOnboarding.EN_COURS)
                .dateDebut(dossier.getDateEntree() == null ? LocalDate.now() : dossier.getDateEntree()).build()));
        var existantes = new EnumMap<TacheOnboarding, OnboardingTache>(TacheOnboarding.class);
        taches.findByOnboardingId(onboarding.getId()).forEach(t -> existantes.put(t.getCode(), t));
        var contrat = contrats.findFirstByDossierIdAndStatut(dossier.getId(), StatutContrat.EN_COURS).orElse(null);
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
            } else if (faites != null) {
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

    static boolean automatiqueFaite(TacheOnboarding code, DossierEmploye dossier, Contrat contrat) {
        return switch (code) {
            case COMPTE -> dossier.getUtilisateur() != null && dossier.getUtilisateur().isActif();
            case CONTRAT_SIGNE -> contrat != null && contrat.isSigne();
            default -> false;
        };
    }

    static Vue calculer(DossierEmploye dossier, Contrat contrat, Onboarding onboarding, List<OnboardingTache> lignes) {
        var parCode = new EnumMap<TacheOnboarding, OnboardingTache>(TacheOnboarding.class);
        lignes.forEach(t -> parCode.put(t.getCode(), t));
        var etapes = new ArrayList<Etape>();
        for (var code : TacheOnboarding.values()) {
            if (code.isAutomatique()) {
                boolean fait = automatiqueFaite(code, dossier, contrat);
                String detail = switch (code) {
                    case COMPTE -> fait ? "Identifiant : " + dossier.getUtilisateur().getEmail() : "Compte absent ou désactivé";
                    case CONTRAT_SIGNE -> contrat == null ? "Aucun contrat en cours : ajoutez-le ci-dessous"
                            : fait ? contrat.getType().name() + " signé le " + contrat.getDateSignature().format(JOUR)
                            : contrat.getType().name() + " en cours non signé : déposez l’exemplaire signé";
                    default -> null;
                };
                etapes.add(new Etape(code, fait, detail));
            } else {
                var t = parCode.get(code);
                boolean fait = t != null && t.getStatut() == StatutTache.FAIT;
                etapes.add(new Etape(code, fait, fait && t.getFaitLe() != null
                        ? "Fait le " + t.getFaitLe().format(HORODATAGE) + (t.getFaitPar() == null ? "" : " par " + t.getFaitPar()) : null));
            }
        }
        boolean toutFait = etapes.stream().allMatch(Etape::fait);
        String termineLe = toutFait && onboarding != null && onboarding.getTermineLe() != null
                ? onboarding.getTermineLe().format(HORODATAGE) : null;
        return new Vue(toutFait ? StatutOnboarding.TERMINE : StatutOnboarding.EN_COURS, etapes, termineLe);
    }
}
