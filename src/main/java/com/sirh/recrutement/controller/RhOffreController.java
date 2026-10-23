package com.sirh.recrutement.controller;

import com.sirh.organisation.repository.PosteRepository;
import com.sirh.recrutement.service.CandidatureService;
import com.sirh.recrutement.service.OffreEmploiService;
import com.sirh.referentiel.repository.DiplomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

@Controller
@RequestMapping("/rh/offres")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RH')")
public class RhOffreController {

    private final OffreEmploiService offreEmploiService;
    private final CandidatureService candidatureService;
    private final PosteRepository posteRepository;
    private final com.sirh.organisation.repository.SectionRepository sectionRepository;
    private final DiplomeRepository diplomeRepository;
    private final com.sirh.organisation.repository.EntrepriseRepository entrepriseRepository;
    private final com.sirh.recrutement.service.OffreEntrepriseService offreEntrepriseService;

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("offres", offreEmploiService.listerToutes());
        return "rh/offres-liste";
    }

    @GetMapping("/nouvelle")
    public String formulaireCreation(@RequestParam(required = false) Long demandeId, Model model) {
        model.addAttribute("entreprises", entrepriseRepository.findAll());
        model.addAttribute("postes", posteRepository.findAll());
        model.addAttribute("sections", sectionRepository.findAll());
        model.addAttribute("diplomes", diplomeRepository.findAll());
        model.addAttribute("demandeId", demandeId);
        return "rh/offre-form";
    }

    @PostMapping
    public String creer(@RequestParam(required = false) Long posteId,
                         @RequestParam(required = false) Long entrepriseId,
                         @RequestParam(defaultValue = "NOUVEAU") String modePoste,
                         @RequestParam(required = false) String nouvelIntitule,
                         @RequestParam(required = false) String categorie,
                         @RequestParam(required = false) Long sectionId,
                         @RequestParam(required = false) String titre,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String missions,
                         @RequestParam(required = false) String profil,
                         @RequestParam(defaultValue = "brouillon") String actionOffre,
                         @RequestParam int nbPostes,
                         @RequestParam(required = false) Integer experienceMinMois,
                         @RequestParam(defaultValue = "60") int seuilAcceptation,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateLimite,
                         @RequestParam(required = false) Set<Long> diplomeIds,
                         @RequestParam(required = false) Long demandeId,
                         @RequestParam(required = false) String piecesRequises,
                         @RequestParam java.util.Map<String, String> saisie, Model model,
                         org.springframework.web.servlet.mvc.support.RedirectAttributes redirection) {
        try {
            verifierAction(actionOffre);
            if (!Set.of("NOUVEAU", "EXISTANT").contains(modePoste))
                throw new com.sirh.commun.exception.BusinessException("Choisissez un mode de création de poste.");
            if ("NOUVEAU".equals(modePoste)) {
                if (titre != null && !titre.isBlank()) nouvelIntitule = titre;
                if (nouvelIntitule == null || nouvelIntitule.isBlank())
                    throw new com.sirh.commun.exception.BusinessException("Saisissez l'intitulé du poste personnalisé.");
                posteId = null;
            } else {
                nouvelIntitule = null;
            }
            description = com.sirh.recrutement.dto.TexteAnnonce.composer(missions, profil, description);
            var offre = offreEntrepriseService.creer(entrepriseId, "publier".equals(actionOffre),
                    posteId, nouvelIntitule, categorie, sectionId, titre, description, nbPostes, experienceMinMois,
                    seuilAcceptation, dateLimite, diplomeIds == null ? Set.of() : diplomeIds, demandeId, piecesRequises);
            redirection.addFlashAttribute("succes", "publier".equals(actionOffre) ? "L’offre a été publiée." : "Le brouillon a été enregistré. Vous pouvez le modifier avant publication.");
            return "redirect:/rh/offres/" + offre.getId();
        } catch (com.sirh.commun.exception.BusinessException | com.sirh.commun.exception.ResourceNotFoundException ex) {
            model.addAttribute("saisie", conserverSaisie(saisie));
            model.addAttribute("diplomesSelectionnes", diplomeIds == null ? Set.of() : diplomeIds);
            model.addAttribute("erreur", ex.getMessage());
            associerErreur(model, ex.getMessage());
            return formulaireCreation(demandeId, model);
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("offre", offreEmploiService.obtenir(id));
        model.addAttribute("candidatures", candidatureService.listerPourOffre(id));
        return "rh/offre-detail";
    }

    @GetMapping("/{id}/modifier")
    public String formulaireModification(@PathVariable Long id, Model model) {
        var offre = offreEmploiService.obtenir(id);
        offreEmploiService.verifierBrouillon(offre);
        if (!model.containsAttribute("saisie")) {
            var saisie = new java.util.HashMap<String, String>();
            saisie.put("titre", offre.getTitre());
            saisie.put("entrepriseId", offre.getEntreprise() == null ? "" : offre.getEntreprise().getId().toString());
            saisie.put("description", offre.getDescription());
            saisie.putAll(com.sirh.recrutement.dto.TexteAnnonce.separer(offre.getDescription()));
            saisie.put("nbPostes", String.valueOf(offre.getNbPostes()));
            saisie.put("experienceMinMois", offre.getExperienceMinMois() == null ? "" : offre.getExperienceMinMois().toString());
            saisie.put("dateLimite", offre.getDateLimite() == null ? "" : offre.getDateLimite().toString());
            saisie.put("piecesRequises", String.join("\n", offre.getListePiecesRequises()));
            model.addAttribute("saisie", saisie);
            model.addAttribute("diplomesSelectionnes", offre.getDiplomesRequis().stream()
                    .map(com.sirh.referentiel.entity.Diplome::getId).collect(java.util.stream.Collectors.toSet()));
        }
        model.addAttribute("offreId", id);
        model.addAttribute("entreprises", entrepriseRepository.findAll());
        model.addAttribute("posteIntitule", offre.getPosteIntitule());
        model.addAttribute("posteSection", offre.getPoste() != null && offre.getPoste().getSection() != null
                ? offre.getPoste().getSection().getNom() : "Sans rattachement");
        model.addAttribute("diplomes", diplomeRepository.findAll());
        return "rh/offre-form";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id, @RequestParam(required = false) String titre,
            @RequestParam(required = false) Long entrepriseId,
            @RequestParam(required = false) String description, @RequestParam int nbPostes,
            @RequestParam(required = false) String missions, @RequestParam(required = false) String profil,
            @RequestParam(defaultValue = "brouillon") String actionOffre,
            @RequestParam(required = false) Integer experienceMinMois,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateLimite,
            @RequestParam(required = false) Set<Long> diplomeIds,
            @RequestParam(required = false) String piecesRequises,
            @RequestParam java.util.Map<String, String> saisie, Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirection) {
        try {
            verifierAction(actionOffre);
            description = com.sirh.recrutement.dto.TexteAnnonce.composer(missions, profil, description);
            offreEntrepriseService.modifier(id, entrepriseId, "publier".equals(actionOffre), titre,
                    description, nbPostes, experienceMinMois, dateLimite,
                    diplomeIds == null ? Set.of() : diplomeIds, piecesRequises);
            redirection.addFlashAttribute("succes", "publier".equals(actionOffre) ? "L’offre a été publiée." : "Les modifications du brouillon ont été enregistrées.");
            return "redirect:/rh/offres/" + id;
        } catch (com.sirh.commun.exception.BusinessException ex) {
            model.addAttribute("erreur", ex.getMessage());
            associerErreur(model, ex.getMessage());
            model.addAttribute("saisie", conserverSaisie(saisie));
            model.addAttribute("diplomesSelectionnes", diplomeIds == null ? Set.of() : diplomeIds);
            return formulaireModification(id, model);
        }
    }

    @PostMapping("/{id}/publier")
    public String publier(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirection) {
        try {
            offreEmploiService.publier(id);
            redirection.addFlashAttribute("succes", "L’offre est publiée. Les candidats peuvent consulter l’annonce et déposer leur dossier jusqu’à la date limite, si elle est renseignée.");
        } catch (com.sirh.commun.exception.BusinessException ex) {
            redirection.addFlashAttribute("erreur", ex.getMessage());
        }
        return "redirect:/rh/offres/" + id;
    }

    private void verifierAction(String action) {
        if (!Set.of("brouillon", "publier").contains(action))
            throw new com.sirh.commun.exception.BusinessException("Choisissez une action d’enregistrement valide.");
    }

    private java.util.Map<String, String> conserverSaisie(java.util.Map<String, String> saisie) {
        var copie = new java.util.HashMap<>(saisie);
        if (!copie.containsKey("missions")) copie.putAll(com.sirh.recrutement.dto.TexteAnnonce.separer(copie.get("description")));
        if (!copie.containsKey("titre") && copie.containsKey("nouvelIntitule")) copie.put("titre", copie.get("nouvelIntitule"));
        return copie;
    }

    private void associerErreur(Model model, String message) {
        String texte = message.toLowerCase(java.util.Locale.FRENCH);
        String champ = texte.contains("entreprise") ? "entrepriseId" : texte.contains("profil") ? "profil" : texte.contains("mission") ? "missions"
                : texte.contains("expérience") ? "experienceMinMois" : texte.contains("nombre") ? "nbPostes"
                : texte.contains("date") ? "dateLimite" : texte.contains("pièce") || texte.contains("libellé") ? "piecesRequises"
                : texte.contains("diplôme") ? "diplomeIds" : texte.contains("section") ? "sectionId"
                : texte.contains("catégorie") ? "categorie" : texte.contains("titre") || texte.contains("intitulé") ? "titre" : "posteId";
        model.addAttribute("erreursChamps", java.util.Map.of(champ, message));
        model.addAttribute("etapeErreur", Set.of("missions", "profil", "experienceMinMois", "diplomeIds").contains(champ) ? 1
                : Set.of("dateLimite", "piecesRequises").contains(champ) ? 2 : 0);
    }

    @PostMapping("/{id}/cloturer")
    public String cloturer(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirection) {
        offreEmploiService.cloturer(id);
        redirection.addFlashAttribute("succes", "L’offre est clôturée. Les nouveaux dépôts sont fermés ; les dossiers reçus restent accessibles.");
        return "redirect:/rh/offres/" + id;
    }
}
