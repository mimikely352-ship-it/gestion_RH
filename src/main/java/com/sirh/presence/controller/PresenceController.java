package com.sirh.presence.controller;
import com.sirh.presence.service.*;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.commun.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.*;
import java.time.*;
import java.util.*;

@Controller @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('EMPLOYE','CHEF_SERVICE','DIRECTION','RH','DG')")
public class PresenceController {
    private final TempsService temps;
    private final PlanningService planning;
    private final AccesPresenceService acces;
    private final DossierEmployeRepository dossiers;
    private final Clock horloge;
    @GetMapping("/presence")
    public String personnel(Authentication auth, Model model,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fin) {
        var d=acces.personnel(auth); preparer(d.getId(),debut,fin,auth,model);
        model.addAttribute("situation",temps.situationDuJour(auth));
        model.addAttribute("pauseDefaut",planning.jour(d.getId(),LocalDate.now(horloge)).getPauseMinutes());
        return "presence/temps";
    }
    @GetMapping("/rh/presence") @PreAuthorize("hasRole('RH')")
    public String rh(Authentication auth, Model model, @RequestParam(required=false) Long dossierId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fin) {
        acces.exigerRh(auth); model.addAttribute("estRh",true);
        model.addAttribute("dossiers",dossiers.findByUtilisateurActifTrueOrderByNomAscPrenomAsc());
        if(dossierId!=null) preparer(dossierId,debut,fin,auth,model);
        return "presence/temps";
    }
    private void preparer(Long id, LocalDate debut, LocalDate fin, Authentication auth, Model model) {
        var maintenant=LocalDate.now(horloge);
        if(debut==null) debut=maintenant.withDayOfMonth(1);
        if(fin==null) fin=debut.withDayOfMonth(debut.lengthOfMonth());
        var lignes=temps.calendrier(id,debut,fin,auth);
        model.addAttribute("dossier",acces.lire(id,auth)); model.addAttribute("debut",debut); model.addAttribute("fin",fin);
        model.addAttribute("jours",lignes); model.addAttribute("aujourdhui",maintenant);
        model.addAttribute("travaillees",lignes.stream().mapToInt(TempsService.Jour::getTravaillees).sum());
        model.addAttribute("retards",lignes.stream().mapToInt(TempsService.Jour::getRetard).sum());
        model.addAttribute("supplementaires",lignes.stream().mapToInt(TempsService.Jour::getValidees).sum());
    }
    private void action(Runnable operation, RedirectAttributes flash, String succes) {
        try { operation.run(); flash.addFlashAttribute("succes",succes); }
        catch(BusinessException e) { flash.addFlashAttribute("erreur",e.getMessage()); }
    }
    @PostMapping("/presence/entree")
    public String entree(Authentication auth, RedirectAttributes flash) {
        action(() -> temps.entrer(auth),flash,"Entrée enregistrée. Pensez à pointer votre sortie aujourd’hui."); return "redirect:/presence";
    }
    @PostMapping("/presence/sortie")
    public String sortie(@RequestParam int pauseMinutes, Authentication auth, RedirectAttributes flash) {
        action(() -> temps.sortir(pauseMinutes,auth),flash,"Sortie enregistrée."); return "redirect:/presence";
    }
    @PostMapping("/rh/presence/regime") @PreAuthorize("hasRole('RH')")
    public String regime(@RequestParam Long dossierId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate depuis,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime debut,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime fin,
            @RequestParam int pauseMinutes, @RequestParam(required=false) Set<Integer> semaine,
            Authentication auth, RedirectAttributes flash) {
        action(() -> planning.regime(dossierId,depuis,debut,fin,pauseMinutes,semaine,auth),flash,
                "Horaires habituels enregistrés à partir du "+depuis.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))+".");
        return "redirect:/rh/presence?dossierId="+dossierId;
    }
    @PostMapping("/rh/presence/planning") @PreAuthorize("hasRole('RH')")
    public String planning(@RequestParam Long dossierId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate jour,
            @RequestParam(defaultValue="false") boolean repos,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime debut,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime fin,
            @RequestParam(defaultValue="0") int pauseMinutes, Authentication auth, RedirectAttributes flash) {
        action(() -> planning.exception(dossierId,jour,repos,debut,fin,pauseMinutes,auth),flash,"Jour du planning modifié.");
        return "redirect:/rh/presence?dossierId="+dossierId;
    }
    @PostMapping("/rh/presence/correction") @PreAuthorize("hasRole('RH')")
    public String corriger(@RequestParam Long dossierId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate jour,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime entree,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime sortie,
            @RequestParam int pauseMinutes, @RequestParam(defaultValue="0") int supplementairesValidees,
            @RequestParam String motif, Authentication auth, RedirectAttributes flash) {
        action(() -> temps.corriger(dossierId,jour,entree,sortie,pauseMinutes,supplementairesValidees,motif,auth),flash,
                "Pointage corrigé ; la correction est inscrite au journal du dossier.");
        return "redirect:/rh/presence?dossierId="+dossierId;
    }
    @GetMapping("/rh/presence/variables.csv") @PreAuthorize("hasRole('RH')")
    @ResponseBody
    public ResponseEntity<byte[]> exporter(@RequestParam Long dossierId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fin, Authentication auth) {
        acces.exigerRh(auth); var d=acces.lire(dossierId,auth);
        var csv=new StringBuilder("\uFEFFSalarie;Date;Minutes_prevues;Minutes_travaillees;Retard_minutes;Depassement_minutes;Supplementaires_validees_minutes;Absence_non_payee_minutes;Absence;Anomalie\r\n");
        temps.calendrier(dossierId,debut,fin,auth).stream().filter(j -> !j.getDate().isAfter(LocalDate.now(horloge))).forEach(j ->
            csv.append(cellule(d.getPrenom()+" "+d.getNom())).append(';').append(j.getDate()).append(';').append(j.getPrevues()).append(';')
                .append(j.getTravaillees()).append(';').append(j.getRetard()).append(';').append(j.getSupplementaires()).append(';')
                .append(j.getValidees()).append(';').append(j.getAbsenceNonPayee()).append(';').append(cellule(j.getAbsence())).append(';').append(cellule(j.getAnomalie())).append("\r\n"));
        return ResponseEntity.ok().contentType(new MediaType("text","csv",java.nio.charset.StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=variables-presence.csv")
                .body(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    private static String cellule(String valeur) {
        if(valeur==null) valeur="";
        if(valeur.stripLeading().matches("^[=+@-].*")) valeur="'"+valeur;
        return "\""+valeur.replace("\"","\"\"")+"\"";
    }
}
