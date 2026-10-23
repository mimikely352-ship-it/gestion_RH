package com.sirh.demande.controller;

import com.sirh.commun.enums.DecisionValidation;
import com.sirh.commun.enums.StatutDemande;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.demande.entity.Demande;
import com.sirh.demande.repository.DemandeRepository;
import com.sirh.demande.service.DemandeService;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/rh/demandes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RH')")
public class RhDemandeController {

    private final DemandeService demandeService;
    private final DemandeRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("aValider", demandeService.listerPourValidationRh());
        model.addAttribute("aExecuter", demandeService.listerPourExecutionRh());
        model.addAttribute("enExecution", demandeRepository.findByStatut(StatutDemande.EN_EXECUTION));
        return "rh/demandes-liste";
    }

    @PostMapping("/{id}/valider")
    public String valider(@PathVariable Long id, Authentication authentication,
                           @RequestParam DecisionValidation decision, @RequestParam(required = false) String commentaire) {
        Long moiId = utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable.")).getId();
        demandeService.valider(id, moiId, decision, commentaire);
        return "redirect:/rh/demandes";
    }

    @PostMapping("/{id}/executer")
    public String executer(@PathVariable Long id) {
        Demande demande = demandeService.marquerEnExecution(id);
        if (demande.getType() == com.sirh.commun.enums.TypeDemande.RECRUTEMENT) {
            return "redirect:/rh/offres/nouvelle?demandeId=" + id;
        }
        return "redirect:/rh/demandes/" + id;
    }

    @PostMapping("/{id}/cloturer-execution")
    public String cloturerExecution(@PathVariable Long id,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateEffet,
            org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            demandeService.marquerExecutee(id, dateEffet);
            flash.addFlashAttribute("succes", "Réalisation confirmée. La demande est exécutée.");
        } catch (com.sirh.commun.exception.BusinessException e) {
            flash.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/rh/demandes/" + id;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("demande", demandeService.obtenir(id));
        model.addAttribute("historique", demandeService.historique(id));
        return "rh/demande-detail";
    }
}
