package com.sirh.recrutement.controller;

import com.sirh.recrutement.dto.OffreDto;
import com.sirh.recrutement.service.OffreEmploiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offres")
@RequiredArgsConstructor
public class OffreApiController {

    private final OffreEmploiService offreEmploiService;

    @GetMapping
    public List<OffreDto> lister() {
        return offreEmploiService.listerPubliees().stream().map(OffreDto::new).toList();
    }

    @GetMapping("/{id}")
    public OffreDto detail(@PathVariable Long id) {
        return new OffreDto(offreEmploiService.obtenirPublique(id));
    }

    @PutMapping("/{id}/publier")
    @PreAuthorize("hasRole('RH')")
    public OffreDto publier(@PathVariable Long id) {
        return new OffreDto(offreEmploiService.publier(id));
    }

    @PutMapping("/{id}/cloturer")
    @PreAuthorize("hasRole('RH')")
    public OffreDto cloturer(@PathVariable Long id) {
        return new OffreDto(offreEmploiService.cloturer(id));
    }
}
