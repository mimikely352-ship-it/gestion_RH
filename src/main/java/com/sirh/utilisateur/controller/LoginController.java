package com.sirh.utilisateur.controller;

import com.sirh.utilisateur.dto.LoginResponse;
import com.sirh.utilisateur.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
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
    public String formulaire() {
        return "commun/login";
    }

    @PostMapping("/login")
    public String connecter(@RequestParam String email,
                             @RequestParam String motDePasse,
                             HttpServletResponse response,
                             Model model) {
        try {
            LoginResponse resultat = authService.authentifier(email, motDePasse);
            authService.poserCookie(response, resultat.getToken());
            return "redirect:" + authService.accueilPour(com.sirh.commun.enums.Role.valueOf(resultat.getRole()));
        } catch (AuthenticationException e) {
            model.addAttribute("erreur", "Email ou mot de passe incorrect.");
            return "commun/login";
        }
    }

    @GetMapping("/deconnexion")
    public String deconnecter(HttpServletResponse response) {
        authService.supprimerCookie(response);
        return "redirect:/login";
    }
}
