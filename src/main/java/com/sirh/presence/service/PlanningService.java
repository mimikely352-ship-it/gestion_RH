package com.sirh.presence.service;
import com.sirh.presence.entity.*;
import com.sirh.presence.repository.*;
import com.sirh.commun.enums.ActionAudit;
import com.sirh.commun.exception.BusinessException;
import lombok.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class PlanningService {
    private final RegimeTravailRepository regimes;
    private final JourPlanningRepository jours;
    private final DemandeCongeRepository conges;
    private final AbsenceRepository absences;
    private final PointageJourRepository pointages;
    private final AccesPresenceService acces;
    private final Clock horloge;
    @Value public static class Horaire {
        LocalTime debut; LocalTime fin; int pauseMinutes; boolean repos;
        public int minutes() { return repos ? 0 : (int)Duration.between(debut, fin).toMinutes() - pauseMinutes; }
    }
    public Map<LocalDate,Horaire> horaires(Long id, LocalDate debut, LocalDate fin) {
        var historique = regimes.findByDossierIdOrderByDepuisDesc(id);
        var exceptions = new HashMap<LocalDate,JourPlanning>();
        jours.findByDossierIdAndJourBetween(id, debut, fin).forEach(j -> exceptions.put(j.getJour(),j));
        var resultat = new LinkedHashMap<LocalDate,Horaire>();
        for (var date = debut; !date.isAfter(fin); date = date.plusDays(1)) {
            var exception = exceptions.get(date);
            if (exception != null) {
                resultat.put(date,new Horaire(exception.getDebut(),exception.getFin(),exception.getPauseMinutes(),exception.isRepos()));
                continue;
            }
            RegimeTravail regime = null;
            for (var r : historique) if (!r.getDepuis().isAfter(date)) { regime = r; break; }
            boolean repos = regime == null ? date.getDayOfWeek().getValue() > 5
                    : !Arrays.asList(regime.getJours().split(",")).contains(String.valueOf(date.getDayOfWeek().getValue()));
            resultat.put(date, new Horaire(regime == null ? LocalTime.of(9,0) : regime.getDebut(),
                    regime == null ? LocalTime.of(18,0) : regime.getFin(), repos ? 0 : regime == null ? 60 : regime.getPauseMinutes(),repos));
        }
        return resultat;
    }
    public Horaire jour(Long id, LocalDate date) { return horaires(id,date,date).get(date); }
    public static void verifierHoraire(LocalTime debut, LocalTime fin, int pause) {
        if (debut == null || fin == null || !fin.isAfter(debut) || pause < 0 || pause >= Duration.between(debut,fin).toMinutes())
            throw new BusinessException("Horaire invalide : fin après début et pause inférieure à la durée.");
    }
    private void verifierDisponibilite(Long id, LocalDate debut, LocalDate fin) {
        if (conges.findByDossierIdAndDebutLessThanEqualAndFinGreaterThanEqual(id,fin,debut).stream().anyMatch(c -> c.getStatut().reserve())
                || !absences.findByDossierIdAndActiveTrueAndDebutLessThanEqualAndFinGreaterThanEqual(id,fin,debut).isEmpty())
            throw new BusinessException("Un congé ou une absence existe sur cette période. Réglez-le avant de modifier le planning.");
        if (!pointages.findByDossierIdAndJourBetweenOrderByJourAsc(id,debut,fin).isEmpty())
            throw new BusinessException("Un pointage existe déjà sur cette période.");
    }
    @Transactional
    public void regime(Long id, LocalDate depuis, LocalTime debut, LocalTime fin, int pause, Set<Integer> semaine, Authentication auth) {
        acces.exigerRh(auth); var d = acces.verrouiller(id); acces.exigerActif(d);
        if (depuis == null || depuis.isBefore(LocalDate.now(horloge))) throw new BusinessException("Le régime prend effet aujourd’hui ou plus tard.");
        verifierHoraire(debut,fin,pause);
        if (semaine == null || semaine.isEmpty() || semaine.stream().anyMatch(j -> j < 1 || j > 7)) throw new BusinessException("Choisissez les jours travaillés.");
        // Un régime déjà programmé plus tard borne la période réellement modifiée.
        var jusquAu = regimes.findByDossierIdOrderByDepuisDesc(id).stream()
                .map(RegimeTravail::getDepuis).filter(date -> date.isAfter(depuis))
                .min(LocalDate::compareTo).map(date -> date.minusDays(1)).orElse(LocalDate.of(9999,12,31));
        verifierDisponibilite(id,depuis,jusquAu);
        var r = regimes.findByDossierIdAndDepuis(id,depuis).orElseGet(RegimeTravail::new);
        r.setDossierId(id); r.setDepuis(depuis); r.setDebut(debut); r.setFin(fin); r.setPauseMinutes(pause);
        r.setJours(semaine.stream().sorted().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));
        regimes.save(r); acces.tracer(id,auth,ActionAudit.PLANNING_MODIFIE,"Régime depuis " + depuis + ", jours " + r.getJours());
    }
    @Transactional
    public void exception(Long id, LocalDate date, boolean repos, LocalTime debut, LocalTime fin, int pause, Authentication auth) {
        acces.exigerRh(auth); var d = acces.verrouiller(id); acces.exigerActif(d);
        if (date == null || date.isBefore(LocalDate.now(horloge))) throw new BusinessException("Modifiez le planning d’aujourd’hui ou à venir.");
        if (!repos) verifierHoraire(debut,fin,pause);
        verifierDisponibilite(id,date,date);
        var j = jours.findByDossierIdAndJour(id,date).orElseGet(JourPlanning::new);
        j.setDossierId(id); j.setJour(date); j.setRepos(repos); j.setDebut(repos ? null : debut); j.setFin(repos ? null : fin); j.setPauseMinutes(repos ? 0 : pause);
        jours.save(j); acces.tracer(id,auth,ActionAudit.PLANNING_MODIFIE,"Exception du " + date + ", repos=" + repos);
    }
}
