package com.sirh.presence.controller;
import com.sirh.presence.service.*;
import com.sirh.presence.entity.StatutConge;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.commun.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.*;
import org.springframework.core.io.Resource;
import java.time.*;

@Controller @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('EMPLOYE','CHEF_SERVICE','DIRECTION','RH','DG')")
public class CongeController {
    private final CongeService conges;
    private final AccesPresenceService acces;
    private final DossierEmployeRepository dossiers;
    private final Clock horloge;
    @GetMapping("/conges")
    public String personnel(Authentication auth, Model model, @RequestParam(required=false) Integer annee) {
        var d=acces.personnel(auth); preparer(d.getId(),annee,auth,model);
        return "presence/conges";
    }
    private void preparer(Long id, Integer annee, Authentication auth, Model model) {
        int exercice=annee==null ? LocalDate.now(horloge).getYear() : annee;
        model.addAttribute("dossier",acces.lire(id,auth)); model.addAttribute("annee",exercice);
        model.addAttribute("types",conges.types()); model.addAttribute("soldes",conges.soldes(id,exercice,auth));
        model.addAttribute("demandes",conges.historique(id,auth));
        model.addAttribute("absences",conges.absences(id,auth).stream().filter(a -> a.getDemandeId()==null).toList());
        model.addAttribute("modeDecompte",conges.modeDecompte());
        model.addAttribute("aujourdhui",LocalDate.now(horloge));
    }
    @GetMapping("/conges/validations") @PreAuthorize("hasAnyRole('CHEF_SERVICE','RH')")
    public String validations(Authentication auth, Model model) {
        model.addAttribute("validations",conges.validations(auth)); return "presence/validations";
    }
    @GetMapping("/rh/conges") @PreAuthorize("hasRole('RH')")
    public String rh(Authentication auth, Model model, @RequestParam(required=false) Long dossierId, @RequestParam(required=false) Integer annee) {
        acces.exigerRh(auth); model.addAttribute("estRh",true);
        model.addAttribute("validations",conges.validations(auth)); model.addAttribute("types",conges.types());
        model.addAttribute("dossiers",dossiers.findByUtilisateurActifTrueOrderByNomAscPrenomAsc());
        if(dossierId!=null) preparer(dossierId,annee,auth,model);
        return "presence/conges-rh";
    }
    private void action(Runnable operation, RedirectAttributes flash, String succes) {
        try { operation.run(); flash.addFlashAttribute("succes",succes); }
        catch(BusinessException e) { flash.addFlashAttribute("erreur",e.getMessage()); }
    }
    @PostMapping("/conges")
    public String demander(@RequestParam Long typeId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam String motif, @RequestParam(required=false) MultipartFile justificatif,
            Authentication auth, RedirectAttributes flash) {
        try {
            var demande=conges.soumettre(typeId,debut,fin,motif,justificatif,auth);
            flash.addFlashAttribute("succes","Demande envoyée : "+demande.getJours()+" jour(s) réservé(s), en attente "
                    +(demande.getStatut()==StatutConge.ATTENTE_MANAGER ? "de votre responsable." : "de la décision des RH."));
        } catch(BusinessException e) {
            flash.addFlashAttribute("erreur",e.getMessage());
            var saisie = new java.util.LinkedHashMap<String,Object>();
            saisie.put("typeId",typeId); saisie.put("debut",debut); saisie.put("fin",fin); saisie.put("motif",motif);
            flash.addFlashAttribute("congeSaisie",saisie);
            if (justificatif != null && !justificatif.isEmpty())
                flash.addFlashAttribute("pieceAReselectionner",true);
            if (debut != null && debut.getYear() >= 2000 && debut.getYear() <= 2200)
                return "redirect:/conges?annee="+debut.getYear();
        }
        return "redirect:/conges";
    }
    @PostMapping("/conges/{id}/decision") @PreAuthorize("hasAnyRole('CHEF_SERVICE','RH')")
    public String decider(@PathVariable Long id, @RequestParam boolean accepter,
            @RequestParam(required=false) String commentaire, Authentication auth, RedirectAttributes flash) {
        boolean rh=acces.utilisateur(auth).getRole()==com.sirh.commun.enums.Role.RH;
        Long dossierId=rh ? conges.dossierDe(id) : null;
        try {
            var demande=conges.decider(id,accepter,commentaire,auth);
            flash.addFlashAttribute("succes",switch(demande.getStatut()) {
                case ATTENTE_RH -> "Demande validée : elle est transmise aux RH pour la décision finale.";
                case ACCEPTE -> "Congé accepté : l’absence est inscrite au planning et les jours sont décomptés.";
                case REFUSE -> "Demande refusée : les jours réservés sont de nouveau disponibles.";
                default -> "Décision enregistrée.";
            });
        } catch(BusinessException e) { flash.addFlashAttribute("erreur",e.getMessage()); }
        // Les RH reviennent sur le dossier du salarié concerné (soldes et historique à jour).
        return rh ? "redirect:/rh/conges?dossierId="+dossierId : "redirect:/conges/validations";
    }
    @PostMapping("/conges/{id}/annuler")
    public String annuler(@PathVariable Long id, @RequestParam String motif, Authentication auth, RedirectAttributes flash) {
        action(() -> conges.annuler(id,motif,auth),flash,"Demande annulée : les jours réservés sont de nouveau disponibles.");
        return "redirect:/conges";
    }
    @PostMapping("/rh/conges/{id}/annuler") @PreAuthorize("hasRole('RH')")
    public String annulerRh(@PathVariable Long id, @RequestParam Long dossierId, @RequestParam String motif, Authentication auth, RedirectAttributes flash) {
        action(() -> conges.annuler(id,motif,auth),flash,"Congé annulé : le salarié est prévenu, les jours sont de nouveau disponibles.");
        return "redirect:/rh/conges?dossierId="+dossierId;
    }
    @PostMapping("/rh/conges/droits") @PreAuthorize("hasRole('RH')")
    public String droits(@RequestParam Long dossierId, @RequestParam Long typeId, @RequestParam int annee,
            @RequestParam int jours, @RequestParam String motif, Authentication auth, RedirectAttributes flash) {
        action(() -> conges.definirDroit(dossierId,typeId,annee,jours,motif,auth),flash,
                "Droits enregistrés : "+jours+" jour(s) pour "+annee+".");
        return "redirect:/rh/conges?dossierId="+dossierId+"&annee="+annee;
    }
    @PostMapping("/rh/conges/types") @PreAuthorize("hasRole('RH')")
    public String type(@RequestParam String nom, @RequestParam(defaultValue="false") boolean deduitSolde,
            @RequestParam(defaultValue="false") boolean remunere, @RequestParam(defaultValue="false") boolean justificatifRequis,
            Authentication auth, RedirectAttributes flash) {
        action(() -> conges.ajouterType(nom,deduitSolde,remunere,justificatifRequis,auth),flash,"Type de congé créé.");
        return "redirect:/rh/conges";
    }
    @PostMapping("/rh/conges/absences") @PreAuthorize("hasRole('RH')")
    public String absence(@RequestParam Long dossierId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam String motif, @RequestParam(defaultValue="false") boolean remuneree,
            @RequestParam(required=false) MultipartFile justificatif, Authentication auth, RedirectAttributes flash) {
        action(() -> conges.absence(dossierId,debut,fin,motif,remuneree,justificatif,auth),flash,
                "Absence enregistrée"+(remuneree ? " (rémunérée)." : " (non rémunérée : reportée dans les variables de paie)."));
        return "redirect:/rh/conges?dossierId="+dossierId;
    }
    @PostMapping("/rh/conges/absences/{id}/annuler") @PreAuthorize("hasRole('RH')")
    public String annulerAbsence(@PathVariable Long id, @RequestParam Long dossierId, @RequestParam String motif, Authentication auth, RedirectAttributes flash) {
        action(() -> conges.annulerAbsence(id,motif,auth),flash,"Absence annulée.");
        return "redirect:/rh/conges?dossierId="+dossierId;
    }
    @GetMapping("/conges/{id}/justificatif") @ResponseBody
    public ResponseEntity<Resource> piece(@PathVariable Long id, Authentication auth) { return pdf(conges.piece(id,false,auth)); }
    @GetMapping("/absences/{id}/justificatif") @ResponseBody
    public ResponseEntity<Resource> pieceAbsence(@PathVariable Long id, Authentication auth) { return pdf(conges.piece(id,true,auth)); }
    private ResponseEntity<Resource> pdf(Resource piece) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=justificatif.pdf").body(piece);
    }
}
