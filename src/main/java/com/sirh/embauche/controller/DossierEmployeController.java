package com.sirh.embauche.controller;

import com.sirh.commun.enums.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.embauche.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Modifications du dossier salarié, réservées aux RH. Chaque action réussie est inscrite
 * au journal d'audit du dossier.
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('RH')")
@RequestMapping("/rh/employes/{dossierId:\\d+}")
public class DossierEmployeController {
    private final ContratService contrats;
    private final OnboardingService onboarding;
    private final DepartService departs;
    private final DossierSalarieService dossierSalarie;
    private final AuditDossierService audit;

    /** Exécute l'action, journalise (le détail est calculé après succès) et revient sur la fiche. */
    private String executer(Long dossierId, Authentication compte, RedirectAttributes flash, String succes, String ancre,
                            ActionAudit action, Supplier<String> operation) {
        try {
            String detail = operation.get();
            audit.journaliser(dossierId, compte, action, detail);
            flash.addFlashAttribute("message", succes);
        } catch (BusinessException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/rh/employes/" + dossierId + "#" + ancre;
    }

    @PostMapping("/identite")
    public String identite(@PathVariable Long dossierId, @ModelAttribute DossierSalarieService.IdentiteForm form,
            Authentication compte, RedirectAttributes flash) {
        try {
            var modifies = dossierSalarie.mettreAJourIdentite(dossierId, form, compte.getName());
            if (modifies.isEmpty()) {
                flash.addFlashAttribute("message", "Aucune modification.");
            } else {
                audit.journaliser(dossierId, compte, ActionAudit.IDENTITE_MODIFIEE, "Champs modifiés : " + String.join(", ", modifies));
                flash.addFlashAttribute("message", "Identité et coordonnées mises à jour (" + String.join(", ", modifies) + ").");
            }
        } catch (BusinessException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/rh/employes/" + dossierId + "#identite";
    }

    @PostMapping("/salaires")
    public String reviserSalaire(@PathVariable Long dossierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEffet,
            @RequestParam(required = false) BigDecimal salaireBrut, @RequestParam(required = false) String motif,
            Authentication compte, RedirectAttributes flash) {
        return executer(dossierId, compte, flash, "Rémunération révisée ; l’historique est conservé.", "remuneration",
                ActionAudit.SALAIRE_REVISE, () -> {
                    var r = dossierSalarie.reviserSalaire(dossierId, dateEffet, salaireBrut, motif, compte.getName());
                    return "Effet au " + r.getDateEffet() + " — " + r.getMotif();
                });
    }

    @PostMapping("/documents")
    public String ajouterDocument(@PathVariable Long dossierId, @RequestParam(required = false) TypeDocumentAdministratif type,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEmission,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateExpiration,
            @RequestParam(name = "fichier", required = false) MultipartFile fichier,
            Authentication compte, RedirectAttributes flash) {
        return executer(dossierId, compte, flash, "Document administratif ajouté.", "documents", ActionAudit.DOCUMENT_AJOUTE, () -> {
            dossierSalarie.ajouterDocument(dossierId, type, numero, dateEmission, dateExpiration, fichier);
            return type.getLibelle();
        });
    }

    @PostMapping("/onboarding")
    public String onboarding(@PathVariable Long dossierId,
            @RequestParam(name = "taches", required = false) Set<TacheOnboarding> taches,
            Authentication compte, RedirectAttributes flash) {
        return executer(dossierId, compte, flash, "Checklist d’intégration enregistrée.", "onboarding", ActionAudit.ONBOARDING_MODIFIE, () -> {
            onboarding.enregistrer(dossierId, taches, compte.getName());
            return taches == null || taches.isEmpty() ? "Aucune tâche manuelle cochée" : "Tâches cochées : " + taches;
        });
    }

    @PostMapping("/contrats")
    public String ajouterContrat(@PathVariable Long dossierId, @RequestParam(required = false) TypeContrat type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFinPrevue,
            @RequestParam(required = false) BigDecimal salaireBrut,
            @RequestParam(name = "fichier", required = false) MultipartFile fichier,
            Authentication compte, RedirectAttributes flash) {
        return executer(dossierId, compte, flash, "Contrat enregistré. L’historique des contrats précédents est conservé.", "contrats",
                ActionAudit.CONTRAT_AJOUTE, () -> {
                    var c = contrats.ajouter(dossierId, type, dateDebut, dateFinPrevue, salaireBrut, fichier, compte.getName());
                    return c.getType() + " à partir du " + c.getDateDebut();
                });
    }

    @PostMapping("/contrats/{contratId:\\d+}/signature")
    public String signerContrat(@PathVariable Long dossierId, @PathVariable Long contratId,
            @RequestParam(name = "fichier", required = false) MultipartFile fichier,
            Authentication compte, RedirectAttributes flash) {
        return executer(dossierId, compte, flash, "Exemplaire signé déposé.", "contrats", ActionAudit.CONTRAT_SIGNE, () -> {
            contrats.joindreExemplaireSigne(dossierId, contratId, fichier);
            return "Contrat n°" + contratId;
        });
    }

    @PostMapping("/departs")
    public String ouvrirDepart(@PathVariable Long dossierId, @RequestParam(required = false) TypeDepart type,
            @RequestParam(required = false) String motif, @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateNotification,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEffet,
            Authentication compte, RedirectAttributes flash) {
        return executer(dossierId, compte, flash, "Dossier de départ ouvert. Déposez les pièces puis clôturez-le à la date d’effet.", "depart",
                ActionAudit.DEPART_OUVERT, () -> {
                    var d = departs.ouvrir(dossierId, type, motif, description, dateNotification, dateEffet, compte.getName());
                    return d.getType().getLibelle() + ", dernier jour le " + d.getDateEffet();
                });
    }

    @PostMapping("/departs/{departId:\\d+}/pieces")
    public String deposerPiece(@PathVariable Long dossierId, @PathVariable Long departId,
            @RequestParam(required = false) PieceDepart piece,
            @RequestParam(name = "fichier", required = false) MultipartFile fichier,
            Authentication compte, RedirectAttributes flash) {
        return executer(dossierId, compte, flash, "Pièce ajoutée au dossier de départ.", "depart", ActionAudit.DEPART_PIECE, () -> {
            departs.deposerPiece(dossierId, departId, piece, fichier);
            return piece.getLibelle();
        });
    }

    @PostMapping("/departs/{departId:\\d+}/cloturer")
    public String cloturerDepart(@PathVariable Long dossierId, @PathVariable Long departId,
            Authentication compte, RedirectAttributes flash) {
        return executer(dossierId, compte, flash, "Départ clôturé : contrat rompu, affectation terminée et compte désactivé.", "depart",
                ActionAudit.DEPART_CLOTURE, () -> { departs.cloturer(dossierId, departId, compte.getName()); return null; });
    }

    @PostMapping("/departs/{departId:\\d+}/annuler")
    public String annulerDepart(@PathVariable Long dossierId, @PathVariable Long departId,
            Authentication compte, RedirectAttributes flash) {
        return executer(dossierId, compte, flash, "Dossier de départ annulé.", "depart",
                ActionAudit.DEPART_ANNULE, () -> { departs.annuler(dossierId, departId, compte.getName()); return null; });
    }
}
