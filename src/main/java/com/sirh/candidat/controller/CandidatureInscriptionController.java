package com.sirh.candidat.controller;

import com.sirh.candidat.dto.InscriptionCandidatForm;
import com.sirh.candidat.service.InscriptionCandidatureService;
import com.sirh.commun.exception.BusinessException;
import com.sirh.recrutement.service.OffreEmploiService;
import com.sirh.referentiel.repository.DiplomeRepository;
import com.sirh.utilisateur.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletResponse;

@Controller @RequiredArgsConstructor
@RequestMapping("/offres/{offreId}/postuler")
public class CandidatureInscriptionController {
    private final InscriptionCandidatureService candidatures;
    private final OffreEmploiService offres;
    private final DiplomeRepository diplomes;
    private final AuthService auth;

    @GetMapping
    public String formulaire(@PathVariable Long offreId, Authentication compte, Model model) {
        var offre = offres.obtenirPublique(offreId);
        if (connecte(compte)) {
            if (role(compte,"CANDIDAT")) return "redirect:/offres/"+offreId+"#depot-titre";
            if (role(compte,"EMPLOYE") && offre.getPoste().isMobiliteOuverte() && offre.getPoste().getSection() != null)
                return "redirect:/employe/demandes/nouvelle?type=TRANSFERT&posteCibleId="+offre.getPoste().getId();
            return "redirect:/offres/"+offreId;
        }
        var form = new InscriptionCandidatForm();
        preparer(offreId,form,model);
        return "public/inscription";
    }

    @PostMapping
    public String envoyer(@PathVariable Long offreId, Authentication compte,
            @jakarta.validation.Valid @ModelAttribute("form") InscriptionCandidatForm form, BindingResult erreurs,
            MultipartHttpServletRequest requete, HttpServletResponse reponse, Model model, RedirectAttributes flash) {
        if (connecte(compte)) throw new BusinessException("Un compte est déjà connecté. Utilisez son parcours de candidature ou de mobilité.");
        if (!erreurs.hasErrors()) {
            try {
                var candidature = candidatures.inscrireEtPostuler(offreId,form,requete.getFileMap());
                auth.connecterCandidatInscrit(reponse,candidature.getCandidat().getUtilisateur());
                flash.addFlashAttribute("succes", "Votre candidature a été envoyée. Retrouvez son avancement ci-dessous.");
                return "redirect:/candidat/mes-candidatures";
            } catch (BusinessException e) {
                model.addAttribute("erreur",e.getMessage()+" Veuillez sélectionner à nouveau vos fichiers.");
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                model.addAttribute("erreur","Un dossier ou un compte existe peut-être déjà. Connectez-vous avec votre compte ou vérifiez votre saisie.");
            }
        } else model.addAttribute("erreur",erreurs.getAllErrors().get(0).getDefaultMessage()+" Veuillez sélectionner à nouveau vos fichiers.");
        form.setMotDePasse(null);
        preparer(offreId,form,model);
        return "public/inscription";
    }

    private void preparer(Long id, InscriptionCandidatForm form, Model model) {
        var offre = offres.obtenirPublique(id);
        if (offre.getDateLimite() != null && offre.getDateLimite().isBefore(java.time.LocalDate.now()))
            throw new BusinessException("Cette offre ne reçoit plus de candidatures.");
        if (form.getExperiences() == null) form.setExperiences(new java.util.ArrayList<>());
        while (form.getExperiences().size() < 3) form.getExperiences().add(new InscriptionCandidatForm.ExperienceForm());
        model.addAttribute("form",form); model.addAttribute("diplomes",diplomes.findAll());
        model.addAttribute("offreCandidature",offre);
        model.addAttribute("actionInscription","/offres/"+id+"/postuler");
    }
    private static boolean connecte(Authentication c) { return c != null && c.isAuthenticated() && !(c instanceof org.springframework.security.authentication.AnonymousAuthenticationToken); }
    private static boolean role(Authentication c,String role) { return c.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_"+role)); }
}
