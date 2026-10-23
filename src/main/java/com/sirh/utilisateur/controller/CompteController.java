package com.sirh.utilisateur.controller;

import com.sirh.utilisateur.service.AuthService;
import com.sirh.commun.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CompteController {
    private final AuthService auth;

    @GetMapping("/compte/accueil")
    public String accueil(Authentication authentication) {
        if (authentication == null) return "redirect:/login";
        var role = authentication.getAuthorities().stream().map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_")).findFirst().orElse("ROLE_CANDIDAT");
        return "redirect:" + auth.accueilPour(Role.valueOf(role.substring(5)));
    }

    @GetMapping("/compte/session")
    @ResponseBody
    public Map<String, String> session(Authentication authentication, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("compte", authentication == null ? "" : authentication.getName());
    }
}
