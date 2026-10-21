package com.sirh.organisation.controller;

import com.sirh.organisation.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Lecture publique de la hiérarchie organisationnelle (filtres en cascade). */
@RestController
@RequestMapping("/api/organisation")
@RequiredArgsConstructor
public class OrganisationApiController {

    private final DirectionRepository directionRepository;
    private final DepartementRepository departementRepository;
    private final ServiceRepository serviceRepository;
    private final SectionRepository sectionRepository;
    private final PosteRepository posteRepository;

    @GetMapping("/directions")
    public List<Map<String, Object>> directions() {
        return directionRepository.findAll().stream()
                .map(d -> Map.<String, Object>of("id", d.getId(), "nom", d.getNom())).toList();
    }

    @GetMapping("/departements")
    public List<Map<String, Object>> departements(@org.springframework.web.bind.annotation.RequestParam(required = false) Long directionId) {
        var liste = directionId == null ? departementRepository.findAll() : departementRepository.findByDirectionId(directionId);
        return liste.stream().map(d -> Map.<String, Object>of("id", d.getId(), "nom", d.getNom())).toList();
    }

    @GetMapping("/services")
    public List<Map<String, Object>> services(@org.springframework.web.bind.annotation.RequestParam(required = false) Long departementId) {
        var liste = departementId == null ? serviceRepository.findAll() : serviceRepository.findByDepartementId(departementId);
        return liste.stream().map(s -> Map.<String, Object>of("id", s.getId(), "nom", s.getNom())).toList();
    }

    @GetMapping("/sections")
    public List<Map<String, Object>> sections(@org.springframework.web.bind.annotation.RequestParam(required = false) Long serviceId) {
        var liste = serviceId == null ? sectionRepository.findAll() : sectionRepository.findByServiceId(serviceId);
        return liste.stream().map(s -> Map.<String, Object>of("id", s.getId(), "nom", s.getNom())).toList();
    }

    @GetMapping("/postes")
    public List<Map<String, Object>> postes(@org.springframework.web.bind.annotation.RequestParam(required = false) Long sectionId) {
        var liste = sectionId == null ? posteRepository.findAll() : posteRepository.findBySectionId(sectionId);
        return liste.stream().map(p -> Map.<String, Object>of("id", p.getId(), "intitule", p.getIntitule())).toList();
    }
}
