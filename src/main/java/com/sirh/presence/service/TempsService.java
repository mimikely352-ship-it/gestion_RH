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
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor
public class TempsService {
    private final AccesPresenceService acces;
    private final PlanningService planning;
    private final PointageJourRepository pointages;
    private final AbsenceRepository absences;
    private final Clock horloge;
    private static final java.time.format.DateTimeFormatter JOUR = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Situation du salarié aujourd'hui, pour n'afficher que l'action de pointage encore possible.
     * ouvertAnterieur : pointage d'un jour passé resté sans sortie (correction RH attendue).
     */
    @Value public static class Situation {
        PointageJour ouvertAnterieur; PointageJour ouvertDuJour; PointageJour termineDuJour;
        String absence; LocalDate entreePrevue;
        public boolean isEntreePossible() {
            return ouvertAnterieur == null && ouvertDuJour == null && termineDuJour == null && absence == null && entreePrevue == null;
        }
        public boolean isSortiePossible() { return ouvertDuJour != null; }
    }

    @Transactional(readOnly=true)
    public Situation situationDuJour(Authentication auth) {
        var d = acces.personnel(auth); var aujourdhui = LocalDate.now(horloge);
        var ouvert = pointages.findFirstByDossierIdAndSortieIsNullOrderByJourAsc(d.getId()).orElse(null);
        var duJour = pointages.findByDossierIdAndJour(d.getId(), aujourdhui).orElse(null);
        String absence = absences.findByDossierIdAndActiveTrueAndDebutLessThanEqualAndFinGreaterThanEqual(d.getId(), aujourdhui, aujourdhui)
                .stream().map(Absence::getLibelle).findFirst().orElse(null);
        LocalDate entreePrevue = d.getDateEntree() != null && aujourdhui.isBefore(d.getDateEntree()) ? d.getDateEntree() : null;
        boolean ouvertAujourdhui = ouvert != null && ouvert.getJour().equals(aujourdhui);
        return new Situation(ouvert != null && !ouvertAujourdhui ? ouvert : null, ouvertAujourdhui ? ouvert : null,
                duJour != null && duJour.getSortie() != null ? duJour : null, absence, entreePrevue);
    }

    @Value public static class Jour {
        LocalDate date; String horaire; String entree; String sortie;
        int prevues; int travaillees; int retard; int supplementaires; int validees; int absenceNonPayee;
        String absence; String anomalie;
    }
    public static int travaillees(PointageJour p) {
        return p.getSortie() == null ? 0 : Math.max(0,(int)Duration.between(p.getEntree(),p.getSortie()).toMinutes()-p.getPauseMinutes());
    }
    public static int supplementaires(PointageJour p) { return Math.max(0,travaillees(p)-p.getMinutesPrevues()); }
    @Transactional(readOnly=true)
    public PointageJour ouvert(Authentication auth) {
        return pointages.findFirstByDossierIdAndSortieIsNullOrderByJourAsc(acces.personnel(auth).getId()).orElse(null);
    }
    @Transactional(readOnly=true)
    public List<Jour> calendrier(Long id, LocalDate debut, LocalDate fin, Authentication auth) {
        var d = acces.lire(id,auth); verifierPeriode(debut,fin);
        var heures = planning.horaires(id,debut,fin);
        var pointagesParDate = new HashMap<LocalDate,PointageJour>();
        pointages.findByDossierIdAndJourBetweenOrderByJourAsc(id,debut,fin).forEach(p -> pointagesParDate.put(p.getJour(),p));
        var absencesPeriode = absences.findByDossierIdAndActiveTrueAndDebutLessThanEqualAndFinGreaterThanEqual(id,fin,debut);
        var resultat = new ArrayList<Jour>();
        heures.forEach((date,h) -> {
            var a = absencesPeriode.stream().filter(x -> !date.isBefore(x.getDebut()) && !date.isAfter(x.getFin())).findFirst().orElse(null);
            var p = pointagesParDate.get(date);
            int prevues = p != null ? p.getMinutesPrevues()
                    : d.getDateEntree() != null && date.isBefore(d.getDateEntree()) ? 0 : h.minutes();
            int faites = p == null ? 0 : travaillees(p);
            int retard = p == null || p.getDebutPrevu() == null ? 0 : Math.max(0,(int)Duration.between(date.atTime(p.getDebutPrevu()),p.getEntree()).toMinutes());
            String anomalie = p == null ? (prevues > 0 && a == null && date.isBefore(LocalDate.now(horloge)) ? "Absence à vérifier" : "")
                    : p.getSortie() == null ? (date.equals(LocalDate.now(horloge)) ? "En cours" : "Sortie manquante") : p.getMinutesPrevues() == 0 ? "Hors planning"
                    : faites < p.getMinutesPrevues() ? "Temps incomplet" : "";
            resultat.add(new Jour(date,h.isRepos() ? "Repos" : h.getDebut()+" – "+h.getFin(),
                    p == null ? "—" : p.getEntree().toLocalTime().toString(),
                    p == null || p.getSortie() == null ? "—" : p.getSortie().toLocalTime().toString(),
                    a == null ? prevues : 0,faites,retard,p == null ? 0 : supplementaires(p),
                    p == null ? 0 : p.getSupplementairesValidees(),a != null && !a.isRemuneree() ? prevues : 0,
                    a == null ? "" : a.getLibelle(),anomalie));
        });
        return resultat;
    }
    public static void verifierPeriode(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null || fin.isBefore(debut) || ChronoUnit.DAYS.between(debut,fin)>61)
            throw new BusinessException("Choisissez une période de 62 jours maximum.");
    }
    @Transactional
    public void entrer(Authentication auth) {
        var personnel = acces.personnel(auth); var d = acces.verrouiller(personnel.getId()); acces.exigerActif(d);
        var maintenant = LocalDateTime.now(horloge).truncatedTo(ChronoUnit.MINUTES); var date = maintenant.toLocalDate();
        if (d.getDateEntree() != null && date.isBefore(d.getDateEntree())) throw new BusinessException("La date d’entrée n’est pas atteinte.");
        var ouvert = pointages.findFirstByDossierIdAndSortieIsNullOrderByJourAsc(d.getId()).orElse(null);
        if (ouvert != null && !ouvert.getJour().equals(date))
            throw new BusinessException("Votre pointage du " + ouvert.getJour().format(JOUR)
                    + " n’a pas de sortie : les RH doivent le corriger avant une nouvelle entrée.");
        if (ouvert != null || pointages.findByDossierIdAndJour(d.getId(),date).isPresent())
            throw new BusinessException("Vous avez déjà pointé aujourd’hui. Pour toute correction, adressez-vous aux RH.");
        verifierAbsence(d.getId(),date);
        var h = planning.jour(d.getId(),date);
        pointages.save(PointageJour.builder().dossierId(d.getId()).jour(date).entree(maintenant)
                .pauseMinutes(h.getPauseMinutes()).minutesPrevues(h.minutes()).debutPrevu(h.isRepos()?null:h.getDebut())
                .modifiePar(auth.getName()).build());
        acces.tracer(d.getId(),auth,ActionAudit.POINTAGE_MODIFIE,"Entrée : " + maintenant);
    }
    @Transactional
    public void sortir(int pause, Authentication auth) {
        var d = acces.verrouiller(acces.personnel(auth).getId()); acces.exigerActif(d);
        var p = pointages.findFirstByDossierIdAndSortieIsNullOrderByJourAsc(d.getId())
                .orElseThrow(() -> new BusinessException("Enregistrez d’abord votre entrée."));
        var sortie = LocalDateTime.now(horloge).truncatedTo(ChronoUnit.MINUTES);
        if (!sortie.toLocalDate().equals(p.getJour()))
            throw new BusinessException("La sortie doit être enregistrée le jour de l’entrée. Demandez une correction aux RH.");
        verifierPointage(p.getJour(),p.getEntree(),sortie,pause);
        p.setSortie(sortie); p.setPauseMinutes(pause); p.setModifiePar(auth.getName()); pointages.save(p);
        acces.tracer(d.getId(),auth,ActionAudit.POINTAGE_MODIFIE,"Sortie : " + sortie + ", pause " + pause + " min");
    }
    private void verifierPointage(LocalDate jour, LocalDateTime entree, LocalDateTime sortie, int pause) {
        if (jour == null || entree == null || !entree.toLocalDate().equals(jour) || entree.isAfter(LocalDateTime.now(horloge))
                || sortie == null || !sortie.isAfter(entree) || sortie.isAfter(entree.plusDays(1))
                || sortie.isAfter(LocalDateTime.now(horloge)) || pause < 0 || pause >= Duration.between(entree,sortie).toMinutes())
            throw new BusinessException("Pointage invalide : durée positive, pause inférieure à la durée, sans date future (24 h maximum).");
    }
    private void verifierAbsence(Long id, LocalDate date) {
        if (!absences.findByDossierIdAndActiveTrueAndDebutLessThanEqualAndFinGreaterThanEqual(id,date,date).isEmpty())
            throw new BusinessException("Une absence ou un congé est enregistré pour ce jour.");
    }
    @Transactional
    public void corriger(Long id, LocalDate jour, LocalTime entree, LocalTime sortie, int pause, int heuresValidees,
            String motif, Authentication auth) {
        acces.exigerRh(auth); var d = acces.verrouiller(id); acces.exigerActif(d);
        if (motif == null || motif.isBlank() || motif.length()>255) throw new BusinessException("Indiquez un motif de correction (255 caractères maximum).");
        if (jour == null || entree == null || sortie == null) throw new BusinessException("Précisez le jour, l’entrée et la sortie.");
        if (d.getDateEntree()!=null && jour.isBefore(d.getDateEntree())) throw new BusinessException("Le pointage précède l’entrée du salarié.");
        verifierPointage(jour,jour.atTime(entree),jour.atTime(sortie),pause); verifierAbsence(id,jour);
        var p = pointages.findByDossierIdAndJour(id,jour).orElseGet(PointageJour::new);
        var avant = p.getEntree()+" / "+p.getSortie()+" / pause "+p.getPauseMinutes();
        var h = planning.jour(id,jour);
        if (p.getId()==null) { p.setMinutesPrevues(h.minutes()); p.setDebutPrevu(h.isRepos()?null:h.getDebut()); }
        p.setDossierId(id); p.setJour(jour); p.setEntree(jour.atTime(entree)); p.setSortie(jour.atTime(sortie)); p.setPauseMinutes(pause);
        if (heuresValidees<0 || heuresValidees>supplementaires(p)) throw new BusinessException("Les minutes supplémentaires validées dépassent le dépassement constaté.");
        p.setSupplementairesValidees(heuresValidees); p.setMotifCorrection(motif.trim()); p.setModifiePar(auth.getName()); pointages.save(p);
        acces.tracer(id,auth,ActionAudit.POINTAGE_MODIFIE,"Avant : "+avant+" ; après : "+entree+" / "+sortie+" ; "+motif.trim());
    }
}
