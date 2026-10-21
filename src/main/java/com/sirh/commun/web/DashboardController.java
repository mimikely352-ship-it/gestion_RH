package com.sirh.commun.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Tableaux de bord minimalistes (liens d'accès rapide) pour chaque rôle interne. */
@Controller
public class DashboardController {

    @GetMapping("/rh/tableau-de-bord")
    @PreAuthorize("hasRole('RH')")
    public String rh() {
        return "rh/tableau-de-bord";
    }

    @GetMapping("/dg/tableau-de-bord")
    @PreAuthorize("hasRole('DG')")
    public String dg() {
        return "dg/tableau-de-bord";
    }

    @GetMapping("/direction/tableau-de-bord")
    @PreAuthorize("hasRole('DIRECTION')")
    public String direction() {
        return "direction/tableau-de-bord";
    }

    @GetMapping("/chef-service/tableau-de-bord")
    @PreAuthorize("hasRole('CHEF_SERVICE')")
    public String chefService() {
        return "chef-service/tableau-de-bord";
    }

    @GetMapping("/employe/tableau-de-bord")
    @PreAuthorize("hasRole('EMPLOYE')")
    public String employe() {
        return "employe/tableau-de-bord";
    }
}
