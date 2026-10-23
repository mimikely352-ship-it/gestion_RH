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
    private final com.sirh.organisation.repository.SectionRepository sectionRepository;
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

    public OffreEmploi obtenirPublique(Long id) {
        var offre = obtenir(id);
        if (offre.getStatut() != StatutOffre.PUBLIEE) throw new ResourceNotFoundException("Offre publiée introuvable.");
        return offre;
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
        if (experienceMinMois != null && experienceMinMois < 0) {
            throw new BusinessException("L’expérience minimale ne peut pas être négative.");
        }
        if (nbPostes < 1) throw new BusinessException("Le nombre de postes doit être positif.");
        Poste poste = posteRepository.findById(posteId)
                .orElseThrow(() -> new ResourceNotFoundException("Poste introuvable : " + posteId));
        titre = titre == null || titre.isBlank() ? poste.getIntitule() : titre.trim();
        if (titre.length() > 150) throw new BusinessException("Le titre est limité à 150 caractères.");
        if (description == null || description.isBlank()) throw new BusinessException("Décrivez les missions du poste.");
        if (dateLimite != null && dateLimite.isBefore(LocalDate.now()))
            throw new BusinessException("La date limite ne peut pas être passée.");
        Set<Diplome> diplomes = new HashSet<>(diplomeRepository.findAllById(diplomeIds));
        if (diplomes.size() != diplomeIds.size()) throw new BusinessException("Un diplôme sélectionné n'existe plus.");
        Demande demandeOrigine = demandeOrigineId == null ? null : demandeService.obtenir(demandeOrigineId);
        if (demandeOrigine != null && (demandeOrigine.getType() != com.sirh.commun.enums.TypeDemande.RECRUTEMENT
                || (demandeOrigine.getStatut() != com.sirh.commun.enums.StatutDemande.VALIDEE_DG
                && demandeOrigine.getStatut() != com.sirh.commun.enums.StatutDemande.EN_EXECUTION)))
            throw new BusinessException("L’offre doit provenir d’une demande de recrutement approuvée et non encore exécutée.");
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
        return offre;
    }

    @Transactional
    public OffreEmploi creerAvecPoste(Long posteId, String nouvelIntitule, String categorie,
            Long sectionId, String titre, String description, int nbPostes,
            Integer experienceMinMois, int seuilAcceptation, LocalDate dateLimite,
            Set<Long> diplomeIds, Long demandeId) {
        if (nouvelIntitule != null && !nouvelIntitule.isBlank()) {
            if (posteId != null) throw new BusinessException("Choisissez un poste existant ou saisissez un nouveau poste.");
            categorie = categorie == null || categorie.isBlank() ? null : categorie.trim();
            if (nouvelIntitule.trim().length() > 150 || (categorie != null && categorie.length() > 150))
                throw new BusinessException("Le poste et la catégorie sont limités à 150 caractères.");
            var section = sectionId == null ? null : sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new BusinessException("Cette section n'existe plus. Choisissez une autre section ou laissez ce champ vide."));
            posteId = posteRepository.save(Poste.builder().intitule(nouvelIntitule.trim())
                    .categorie(categorie).section(section).build()).getId();
        }
        if (posteId == null) throw new BusinessException("Choisissez ou créez un poste.");
        return creer(posteId, titre, description, nbPostes, experienceMinMois,
                seuilAcceptation, dateLimite, diplomeIds, demandeId);
    }

    @Transactional
    public OffreEmploi modifierBrouillon(Long id, String titre, String description, int nbPostes,
            Integer experienceMinMois, LocalDate dateLimite, Set<Long> diplomeIds) {
        OffreEmploi offre = obtenir(id);
        verifierBrouillon(offre);
        titre = titre == null || titre.isBlank() ? offre.getPosteIntitule() : titre.trim();
        if (titre.length() > 150) throw new BusinessException("Le titre est limité à 150 caractères.");
        if (description == null || description.isBlank()) throw new BusinessException("Décrivez les missions du poste.");
        if (nbPostes < 1) throw new BusinessException("Le nombre de postes doit être positif.");
        if (experienceMinMois != null && experienceMinMois < 0)
            throw new BusinessException("L’expérience minimale ne peut pas être négative.");
        if (dateLimite != null && dateLimite.isBefore(LocalDate.now()))
            throw new BusinessException("La date limite ne peut pas être passée.");
        Set<Diplome> diplomes = new HashSet<>(diplomeRepository.findAllById(diplomeIds));
        if (diplomes.size() != diplomeIds.size()) throw new BusinessException("Un diplôme sélectionné n'existe plus.");
        offre.setTitre(titre);
        offre.setDescription(description.trim());
        offre.setNbPostes(nbPostes);
        offre.setExperienceMinMois(experienceMinMois);
        offre.setDateLimite(dateLimite);
        offre.setDiplomesRequis(diplomes);
        return offreEmploiRepository.save(offre);
    }

    public void verifierBrouillon(OffreEmploi offre) {
        if (offre.getStatut() != StatutOffre.BROUILLON)
            throw new BusinessException("Seul un brouillon peut être modifié. Les offres publiées conservent leurs critères de recrutement.");
    }

    public Set<String> lirePiecesRequises(String texte) {
        var pieces = new java.util.TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        if (texte != null) texte.lines().map(String::trim).filter(s -> !s.isEmpty()).forEach(pieces::add);
        if (pieces.size() > 8 || pieces.stream().anyMatch(p -> p.length() > 100))
            throw new BusinessException("Prévoyez au maximum 8 pièces, avec un libellé de 100 caractères maximum chacune.");
        return pieces;
    }

    @Transactional
    public OffreEmploi creerAvecDossier(Long posteId, String nouvelIntitule, String categorie,
            Long sectionId, String titre, String description, int nbPostes,
            Integer experienceMinMois, int seuilAcceptation, LocalDate dateLimite,
            Set<Long> diplomeIds, Long demandeId, String piecesRequises) {
        Set<String> pieces = lirePiecesRequises(piecesRequises);
        OffreEmploi offre = creerAvecPoste(posteId, nouvelIntitule, categorie, sectionId, titre,
                description, nbPostes, experienceMinMois, seuilAcceptation, dateLimite, diplomeIds, demandeId);
        offre.setPiecesRequises(pieces);
        return offreEmploiRepository.save(offre);
    }

    @Transactional
    public OffreEmploi modifierAvecDossier(Long id, String titre, String description, int nbPostes,
            Integer experienceMinMois, LocalDate dateLimite, Set<Long> diplomeIds, String piecesRequises) {
        Set<String> pieces = lirePiecesRequises(piecesRequises);
        OffreEmploi offre = modifierBrouillon(id, titre, description, nbPostes, experienceMinMois, dateLimite, diplomeIds);
        offre.setPiecesRequises(pieces);
        return offreEmploiRepository.save(offre);
    }

    @Transactional
    public OffreEmploi publier(Long offreId) {
        OffreEmploi offre = obtenir(offreId);
        if (offre.getStatut() != StatutOffre.BROUILLON) {
            throw new BusinessException("Seule une offre en brouillon peut être publiée.");
        }
        if (offre.getEntreprise() == null)
            throw new BusinessException("Sélectionnez l’entreprise qui recrute avant publication.");
        if (offre.getDateLimite() != null && offre.getDateLimite().isBefore(LocalDate.now()))
            throw new BusinessException("La date limite est passée. Modifiez le brouillon avant publication.");
        offre.setStatut(StatutOffre.PUBLIEE);
        offre.setDatePublication(LocalDate.now());
        if (offre.getDemandeOrigine() != null) {
            if (offre.getDemandeOrigine().getType() != com.sirh.commun.enums.TypeDemande.RECRUTEMENT)
                throw new BusinessException("Seule une demande de recrutement peut être associée à une offre.");
            demandeService.marquerExecutee(offre.getDemandeOrigine().getId());
        }
        return offreEmploiRepository.save(offre);
    }

    @Transactional
    public OffreEmploi creerEtPublier(Long posteId, String nouvelIntitule, String categorie,
            Long sectionId, String titre, String description, int nbPostes,
            Integer experienceMinMois, int seuilAcceptation, LocalDate dateLimite,
            Set<Long> diplomeIds, Long demandeId, String piecesRequises) {
        var offre = creerAvecDossier(posteId, nouvelIntitule, categorie, sectionId, titre, description,
                nbPostes, experienceMinMois, seuilAcceptation, dateLimite, diplomeIds, demandeId, piecesRequises);
        return publier(offre.getId());
    }

    @Transactional
    public OffreEmploi modifierEtPublier(Long id, String titre, String description, int nbPostes,
            Integer experienceMinMois, LocalDate dateLimite, Set<Long> diplomeIds, String piecesRequises) {
        var offre = modifierAvecDossier(id, titre, description, nbPostes, experienceMinMois,
                dateLimite, diplomeIds, piecesRequises);
        return publier(offre.getId());
    }

    @Transactional
    public OffreEmploi cloturer(Long offreId) {
        OffreEmploi offre = obtenir(offreId);
        offre.setStatut(StatutOffre.CLOTUREE);
        return offreEmploiRepository.save(offre);
    }
}
