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

import java.util.List;

@Controller
@RequestMapping("/chef-service/demandes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CHEF_SERVICE')")
public class ChefServiceDemandeController {

    private final DemandeService demandeService;
    private final DemandeRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;

    // Cette méthode prépare la liste des demandes qu’un chef de service doit examiner.
    
    @GetMapping
    public String liste(Authentication authentication, Model model) {
        // Le code récupère d’abord l’identifiant de l’utilisateur connecté à partir de son email. Si cet utilisateur n’existe pas, il déclenche l’erreur « Utilisateur introuvable ».
        Long moiId = utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable.")).getId();
        // il récupère les demandes dont le statut est SOUMISE, puis ne garde que celles dont l’auteur appartient à un service dirigé par cet utilisateur
        List<Demande> aValider = demandeRepository.findByStatut(StatutDemande.SOUMISE).stream()
                .filter(d -> d.getType() != com.sirh.commun.enums.TypeDemande.TRANSFERT)
                .filter(d -> d.getAuteur().getService() != null
                        && d.getAuteur().getService().getChefService() != null
                        && d.getAuteur().getService().getChefService().getId().equals(moiId))
                .toList();
        // l place cette liste dans le modèle sous le nom demandes et affiche la page chef-service/demandes-liste
        model.addAttribute("demandes", aValider);
        return "chef-service/demandes-liste";
    }

    // Cette méthode traite la décision d’un chef de service sur une demande, puis revient à la liste des demandes
    @PostMapping("/{id}/valider")
    public String valider(@PathVariable Long id, Authentication authentication,
                           @RequestParam DecisionValidation decision, @RequestParam(required = false) String commentaire) {
        Long moiId = utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable.")).getId();
        demandeService.valider(id, moiId, decision, commentaire);
        return "redirect:/chef-service/demandes";
    }
}
