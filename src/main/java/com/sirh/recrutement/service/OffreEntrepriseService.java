package com.sirh.recrutement.service;

import com.sirh.commun.exception.BusinessException;
import com.sirh.organisation.repository.EntrepriseRepository;
import com.sirh.recrutement.entity.OffreEmploi;
import com.sirh.recrutement.repository.OffreEmploiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Set;

@Service @RequiredArgsConstructor
public class OffreEntrepriseService {
    private final EntrepriseRepository entreprises;
    private final OffreEmploiRepository offres;
    private final OffreEmploiService service;

    @Transactional
    public OffreEmploi creer(Long entrepriseId, boolean publier, Long posteId, String nouvelIntitule,
            String categorie, Long sectionId, String titre, String description, int nbPostes,
            Integer experienceMinMois, int seuil, LocalDate dateLimite, Set<Long> diplomes,
            Long demandeId, String pieces) {
        var entreprise = obtenirEntreprise(entrepriseId);
        var offre = service.creerAvecDossier(posteId, nouvelIntitule, categorie, sectionId, titre,
                description, nbPostes, experienceMinMois, seuil, dateLimite, diplomes, demandeId, pieces);
        offre.setEntreprise(entreprise);
        offres.save(offre);
        return publier ? service.publier(offre.getId()) : offre;
    }

    @Transactional
    public void modifier(Long id, Long entrepriseId, boolean publier, String titre, String description,
            int nbPostes, Integer experience, LocalDate dateLimite, Set<Long> diplomes, String pieces) {
        var entreprise = obtenirEntreprise(entrepriseId);
        var offre = service.modifierAvecDossier(id, titre, description, nbPostes, experience, dateLimite, diplomes, pieces);
        offre.setEntreprise(entreprise);
        offres.save(offre);
        if (publier) service.publier(id);
    }

    private com.sirh.organisation.entity.Entreprise obtenirEntreprise(Long id) {
        if (id == null) throw new BusinessException("Sélectionnez l’entreprise qui recrute.");
        return entreprises.findById(id).orElseThrow(() -> new BusinessException("Cette entreprise n’existe plus."));
    }
}
