package com.sirh.utilisateur.controller;

import com.sirh.utilisateur.dto.LoginResponse;
import com.sirh.utilisateur.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final AuthService authService;

    @GetMapping("/login")
    public String formulaire(@RequestParam(required = false) Long offreId, Model model) {
        model.addAttribute("offreId",offreId);
        return "commun/login";
    }

    @PostMapping("/login")
    public String connecter(@RequestParam String email,
                             @RequestParam String motDePasse,
                             @RequestParam(required = false) Long offreId,
                             HttpServletResponse response,
                             Model model) {
        try {
            LoginResponse resultat = authService.authentifier(email, motDePasse);
            authService.poserCookie(response, resultat.getToken());
            if (offreId != null && "CANDIDAT".equals(resultat.getRole())) return "redirect:/offres/"+offreId+"#depot-titre";
            return "redirect:" + authService.accueilPour(com.sirh.commun.enums.Role.valueOf(resultat.getRole()));
        } catch (AuthenticationException e) {
            model.addAttribute("erreur", "Email ou mot de passe incorrect.");
            model.addAttribute("email", email);
            model.addAttribute("offreId",offreId);
            return "commun/login";
        }
    }

    @PostMapping("/deconnexion")
    public String deconnecter(HttpServletRequest request, HttpServletResponse response) {
        var compte = SecurityContextHolder.getContext().getAuthentication();
        if (compte != null && compte.isAuthenticated()) authService.revoquer(compte.getName());
        authService.supprimerCookie(response);
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }
}
