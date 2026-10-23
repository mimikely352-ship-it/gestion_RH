package com.sirh.ordre.controller;

import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.ordre.dto.OrdreDto;
import com.sirh.ordre.service.OrdreService;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordres")
@RequiredArgsConstructor
public class OrdreApiController {

    private final OrdreService ordreService;
    private final UtilisateurRepository utilisateurRepository;

    @PostMapping
    @PreAuthorize("hasRole('DG')")
    public OrdreDto emettre(Authentication authentication, @RequestParam Long rhId,
                            @RequestParam String description, @RequestParam(defaultValue = "7") int delaiJours) {
        Long dgId = utilisateurId(authentication);
        return new OrdreDto(ordreService.emettre(dgId, rhId, description, delaiJours));
    }

    @GetMapping("/moi")
    @PreAuthorize("hasRole('RH')")
    public List<OrdreDto> mesOrdres(Authentication authentication) {
        Long rhId = utilisateurId(authentication);
        return ordreService.listerPourRh(rhId).stream().map(OrdreDto::new).toList();
    }

    @PutMapping("/{id}/executer")
    @PreAuthorize("hasRole('RH')")
    public OrdreDto executer(@PathVariable Long id, Authentication authentication) {
        return new OrdreDto(ordreService.marquerExecute(id, authentication.getName()));
    }

    @PutMapping("/{id}/demarrer") @PreAuthorize("hasRole('RH')")
    public OrdreDto demarrer(@PathVariable Long id, Authentication authentication) {
        return new OrdreDto(ordreService.marquerEnExecution(id, authentication.getName()));
    }

    private Long utilisateurId(Authentication authentication) {
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable.")).getId();
    }
}
