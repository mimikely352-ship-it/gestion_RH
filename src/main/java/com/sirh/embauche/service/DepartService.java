package com.sirh.embauche.service;

import com.sirh.candidat.repository.DocumentRepository;
import com.sirh.commun.enums.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.embauche.entity.Depart;
import com.sirh.embauche.repository.AffectationRepository;
import com.sirh.embauche.repository.ContratRepository;
import com.sirh.embauche.repository.DepartRepository;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Démission ou renvoi d'un employé. Le dossier est ouvert (EN_COURS), les pièces y sont déposées,
 * puis la clôture — possible à la date d'effet, pièces obligatoires réunies — rompt le contrat en cours,
 * termine l'affectation et désactive le compte de connexion.
 */
@Service
@RequiredArgsConstructor
public class DepartService {
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DepartRepository departs;
    private final DossierEmployeRepository dossiers;
    private final ContratRepository contrats;
    private final AffectationRepository affectations;
    private final UtilisateurRepository utilisateurs;
    private final DocumentRepository documentsRepository;
    private final DocumentEmployeService documents;

    @Transactional
    public Depart ouvrir(Long dossierId, TypeDepart type, String motif, String description,
                         LocalDate dateNotification, LocalDate dateEffet, String auteur) {
        var dossier = dossiers.verrouillerParId(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + dossierId));
        if (!dossier.getUtilisateur().isActif()) throw new BusinessException("Cet employé a déjà quitté l’entreprise.");
        if (departs.findFirstByDossierIdAndStatut(dossierId, StatutDepart.EN_COURS).isPresent())
            throw new BusinessException("Un dossier de départ est déjà en cours pour cet employé.");
        if (type == null) throw new BusinessException("Choisissez le type de départ.");
        if (dateNotification == null || dateEffet == null)
            throw new BusinessException("Indiquez la date de notification et le dernier jour dans l’entreprise.");
        if (dateNotification.isAfter(LocalDate.now()))
            throw new BusinessException("La date de notification ne peut pas être dans le futur.");
        if (dateEffet.isBefore(dateNotification))
            throw new BusinessException("Le dernier jour ne peut pas précéder la date de notification.");
        String motifRetenu = null;
        if (type == TypeDepart.LICENCIEMENT) {
            if (motif == null || Arrays.stream(MotifLicenciement.values()).noneMatch(m -> m.name().equals(motif)))
                throw new BusinessException("Choisissez le motif du renvoi.");
            motifRetenu = motif;
        }
        return departs.save(Depart.builder().dossier(dossier).type(type).motif(motifRetenu)
                .description(description == null || description.isBlank() ? null : description.trim())
                .dateNotification(dateNotification).dateEffet(dateEffet).statut(StatutDepart.EN_COURS)
                .creeLe(LocalDateTime.now()).creePar(auteur).build());
    }

    @Transactional
    public void deposerPiece(Long dossierId, Long departId, PieceDepart piece, MultipartFile fichier) {
        var depart = enCours(dossierId, departId);
        if (piece == null || !piece.concerne(depart.getType()))
            throw new BusinessException("Cette pièce ne fait pas partie d’un dossier de " + depart.getType().getLibelle().toLowerCase() + ".");
        documents.deposer(fichier, piece.name(), piece.getLibelle(), dossierId, null, departId);
    }

    @Transactional
    public void cloturer(Long dossierId, Long departId, String auteur) {
        var depart = enCours(dossierId, departId);
        if (depart.getDateEffet().isAfter(LocalDate.now()))
            throw new BusinessException("Le départ prend effet le " + depart.getDateEffet().format(JOUR)
                    + " : la clôture sera possible à partir de cette date.");
        var manquantes = piecesManquantes(depart);
        if (!manquantes.isEmpty())
            throw new BusinessException("Pièces obligatoires manquantes : " + String.join(", ", manquantes) + ".");

        String libelle = depart.getType().getLibelle()
                + (depart.getMotif() == null ? "" : " — " + MotifLicenciement.valueOf(depart.getMotif()).getLibelle());
        contrats.findFirstByDossierIdAndStatut(dossierId, StatutContrat.EN_COURS).ifPresent(c -> {
            c.setStatut(StatutContrat.ROMPU);
            c.setDateFinEffective(depart.getDateEffet().isBefore(c.getDateDebut()) ? c.getDateDebut() : depart.getDateEffet());
            c.setMotifFin(libelle);
            contrats.save(c);
        });
        affectations.findByDossierIdAndActuelleTrue(dossierId).ifPresent(a -> {
            a.setActuelle(false);
            a.setDateFin(a.getDateDebut() != null && depart.getDateEffet().isBefore(a.getDateDebut()) ? a.getDateDebut() : depart.getDateEffet());
            affectations.save(a);
        });
        var utilisateur = depart.getDossier().getUtilisateur();
        utilisateur.setActif(false);
        utilisateur.setTokensValidesDepuis(LocalDateTime.now(ZoneOffset.UTC)); // déconnecte les sessions ouvertes
        utilisateurs.save(utilisateur);

        depart.setStatut(StatutDepart.CLOTURE);
        depart.setClotureLe(LocalDateTime.now());
        depart.setCloturePar(auteur);
        departs.save(depart);
    }

    @Transactional
    public void annuler(Long dossierId, Long departId, String auteur) {
        var depart = enCours(dossierId, departId);
        depart.setStatut(StatutDepart.ANNULE);
        depart.setClotureLe(LocalDateTime.now());
        depart.setCloturePar(auteur);
        departs.save(depart);
    }

    @Transactional(readOnly = true)
    public List<Depart> recents() {
        return departs.findTop100ByStatutInOrderByDateEffetDescIdDesc(List.of(StatutDepart.EN_COURS, StatutDepart.CLOTURE));
    }

    List<String> piecesManquantes(Depart depart) {
        var deposees = documentsRepository.findByDepartId(depart.getId()).stream()
                .map(d -> d.getType()).collect(Collectors.toSet());
        return PieceDepart.pour(depart.getType()).stream()
                .filter(p -> p.isObligatoire() && !deposees.contains(p.name()))
                .map(PieceDepart::getLibelle).toList();
    }

    private Depart enCours(Long dossierId, Long departId) {
        var depart = departs.findById(departId)
                .filter(d -> d.getDossier().getId().equals(dossierId))
                .orElseThrow(() -> new ResourceNotFoundException("Dossier de départ introuvable pour cet employé."));
        if (depart.getStatut() != StatutDepart.EN_COURS)
            throw new BusinessException("Ce dossier de départ est déjà clôturé ou annulé.");
        return depart;
    }
}
