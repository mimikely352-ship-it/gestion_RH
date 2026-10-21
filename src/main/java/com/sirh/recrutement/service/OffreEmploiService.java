package com.sirh.recrutement.service;

import com.sirh.commun.enums.StatutOffre;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.demande.entity.Demande;
import com.sirh.demande.service.DemandeService;
import com.sirh.organisation.entity.Poste;
import com.sirh.organisation.repository.PosteRepository;
import com.sirh.recrutement.entity.OffreEmploi;
import com.sirh.recrutement.repository.OffreEmploiRepository;
import com.sirh.referentiel.entity.Diplome;
import com.sirh.referentiel.repository.DiplomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OffreEmploiService {

    private final OffreEmploiRepository offreEmploiRepository;
    private final PosteRepository posteRepository;
    private final DiplomeRepository diplomeRepository;
    private final DemandeService demandeService;

    public List<OffreEmploi> listerPubliees() {
        return offreEmploiRepository.findByStatut(StatutOffre.PUBLIEE);
    }

    public List<OffreEmploi> listerToutes() {
        return offreEmploiRepository.findAll();
    }

    public OffreEmploi obtenir(Long id) {
        return offreEmploiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable : " + id));
    }

    @Transactional
    public OffreEmploi creer(Long posteId, String titre, String description, int nbPostes,
                              Integer experienceMinMois, int seuilAcceptation, LocalDate dateLimite,
                              Set<Long> diplomeIds) {
        return creer(posteId, titre, description, nbPostes, experienceMinMois, seuilAcceptation, dateLimite,
                diplomeIds, null);
    }

    /** Variante utilisée quand l'offre est créée en exécution d'une demande de recrutement validée. */
    @Transactional
    public OffreEmploi creer(Long posteId, String titre, String description, int nbPostes,
                              Integer experienceMinMois, int seuilAcceptation, LocalDate dateLimite,
                              Set<Long> diplomeIds, Long demandeOrigineId) {
        Poste poste = posteRepository.findById(posteId)
                .orElseThrow(() -> new ResourceNotFoundException("Poste introuvable : " + posteId));
        Set<Diplome> diplomes = new HashSet<>(diplomeRepository.findAllById(diplomeIds));
        Demande demandeOrigine = demandeOrigineId == null ? null : demandeService.obtenir(demandeOrigineId);
        OffreEmploi offre = OffreEmploi.builder()
                .poste(poste)
                .demandeOrigine(demandeOrigine)
                .titre(titre)
                .description(description)
                .nbPostes(nbPostes)
                .experienceMinMois(experienceMinMois)
                .seuilAcceptation(seuilAcceptation)
                .dateLimite(dateLimite)
                .statut(StatutOffre.BROUILLON)
                .diplomesRequis(diplomes)
                .build();
        offre = offreEmploiRepository.save(offre);
        if (demandeOrigine != null) {
            demandeService.marquerExecutee(demandeOrigineId);
        }
        return offre;
    }

    @Transactional
    public OffreEmploi publier(Long offreId) {
        OffreEmploi offre = obtenir(offreId);
        if (offre.getStatut() != StatutOffre.BROUILLON) {
            throw new BusinessException("Seule une offre en brouillon peut être publiée.");
        }
        offre.setStatut(StatutOffre.PUBLIEE);
        offre.setDatePublication(LocalDate.now());
        return offreEmploiRepository.save(offre);
    }

    @Transactional
    public OffreEmploi cloturer(Long offreId) {
        OffreEmploi offre = obtenir(offreId);
        offre.setStatut(StatutOffre.CLOTUREE);
        return offreEmploiRepository.save(offre);
    }
}
