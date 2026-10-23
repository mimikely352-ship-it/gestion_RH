package com.sirh.candidat.service;

import com.sirh.candidat.dto.InscriptionCandidatForm;
import com.sirh.commun.enums.StatutOffre;
import com.sirh.commun.exception.BusinessException;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.service.OffreEmploiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.*;

/** Une seule transaction pour le compte, le profil, les documents et la candidature. */
@Service @RequiredArgsConstructor
public class InscriptionCandidatureService {
    private final CandidatInscriptionService inscriptions;
    private final DossierCandidatureService dossiers;
    private final OffreEmploiService offres;
    private final FileStorageService stockage;

    @Transactional
    public Candidature inscrireEtPostuler(Long offreId, InscriptionCandidatForm form, Map<String, MultipartFile> fichiers) {
        var offre = offres.obtenirPublique(offreId);
        if (offre.getStatut() != StatutOffre.PUBLIEE || (offre.getDateLimite() != null && offre.getDateLimite().isBefore(LocalDate.now())))
            throw new BusinessException("Cette offre ne reçoit plus de candidatures.");
        stockage.verifierPdf(form.getCv());
        fichiers = fichiers == null ? Map.of() : fichiers;
        var pieces = offre.getListePiecesRequises();
        for (int i = 0; i < pieces.size(); i++) {
            var fichier = fichiers.get("piece_" + i);
            if (fichier == null || fichier.isEmpty()) throw new BusinessException("Pièce manquante : " + pieces.get(i));
            stockage.verifierPdf(fichier);
        }
        var experiences = form.getExperiences() == null ? List.<CandidatInscriptionService.ExperienceSaisie>of() : form.getExperiences().stream()
                .filter(e -> e != null && e.getIntitule() != null && !e.getIntitule().isBlank())
                .map(e -> new CandidatInscriptionService.ExperienceSaisie(e.getIntitule(),e.getEntreprise(),e.getDateDebut(),e.getDateFin(),e.getDescription())).toList();
        var candidat = inscriptions.inscrire(form.getEmail(), form.getMotDePasse(), form.getNom(), form.getPrenom(),
                form.getTelephone(), form.getDateNaissance(), form.getLieuNaissance(), form.getCin(), form.getAdresse(),
                experiences, form.getDiplomeIds(), form.getCv());
        return dossiers.deposer(candidat.getId(), offreId, fichiers);
    }
}
