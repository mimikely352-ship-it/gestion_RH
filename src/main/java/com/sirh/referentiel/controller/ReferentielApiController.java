package com.sirh.referentiel.controller;

import com.sirh.referentiel.repository.DiplomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/referentiels")
@RequiredArgsConstructor
public class ReferentielApiController {

    private final DiplomeRepository diplomeRepository;

    @GetMapping("/diplomes")
    public List<Map<String, Object>> diplomes() {
        return diplomeRepository.findAll().stream()
                .map(d -> Map.<String, Object>of("id", d.getId(), "libelle", d.getLibelle()))
                .toList();
    }
}
