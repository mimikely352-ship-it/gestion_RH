package com.sirh.embauche.service;

import com.sirh.candidat.repository.DocumentRepository;
import com.sirh.commun.enums.StatutContrat;
import com.sirh.commun.enums.TypeDocumentAdministratif;
import com.sirh.commun.exception.BusinessException;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.embauche.entity.Remuneration;
import com.sirh.embauche.repository.ContratRepository;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.embauche.repository.RemunerationRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Référentiel du dossier salarié : identité, coordonnées, rémunération et documents administratifs. */
@Service
@RequiredArgsConstructor
public class DossierSalarieService {
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern TELEPHONE = Pattern.compile("[0-9 +().-]{6,30}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    public static final List<String> SEXES = List.of("F", "M");
    public static final List<String> SITUATIONS_FAMILIALES = List.of("CELIBATAIRE", "MARIE", "UNION_LIBRE", "DIVORCE", "VEUF");

    private final DossierEmployeRepository dossiers;
    private final RemunerationRepository remunerations;
    private final ContratRepository contrats;
    private final DocumentRepository documentsRepository;
    private final DocumentEmployeService documents;

    /** Saisie RH de l'identité et des coordonnées. */
    @Data
    public static class IdentiteForm {
        private String nom, prenom, sexe, lieuNaissance, nationalite, cin, cinDelivreA, situationFamiliale, numeroCnaps;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate dateNaissance;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate cinDelivreLe;
        private Integer nombreEnfants;
        private String adresse, telephone, emailPersonnel, contactUrgenceNom, contactUrgenceTelephone;
    }

    /** @return les libellés des champs réellement modifiés (pour le journal d'audit, sans les valeurs). */
    @Transactional
    public List<String> mettreAJourIdentite(Long dossierId, IdentiteForm f, String auteur) {
        var d = dossiers.verrouillerParId(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + dossierId));
        String nom = texte(f.getNom()), prenom = texte(f.getPrenom());
        if (nom == null || prenom == null) throw new BusinessException("Le nom et le prénom sont obligatoires.");
        String sexe = texte(f.getSexe());
        if (sexe != null && !SEXES.contains(sexe)) throw new BusinessException("Sexe invalide.");
        String situation = texte(f.getSituationFamiliale());
        if (situation != null && !SITUATIONS_FAMILIALES.contains(situation)) throw new BusinessException("Situation familiale invalide.");
        if (f.getDateNaissance() != null && !f.getDateNaissance().isBefore(LocalDate.now().minusYears(14)))
            throw new BusinessException("La date de naissance n’est pas plausible pour un salarié.");
        if (f.getCinDelivreLe() != null && f.getCinDelivreLe().isAfter(LocalDate.now()))
            throw new BusinessException("La date de délivrance de la CIN ne peut pas être dans le futur.");
        if (f.getNombreEnfants() != null && (f.getNombreEnfants() < 0 || f.getNombreEnfants() > 30))
            throw new BusinessException("Nombre d’enfants invalide.");
        for (var tel : new String[]{texte(f.getTelephone()), texte(f.getContactUrgenceTelephone())})
            if (tel != null && !TELEPHONE.matcher(tel).matches()) throw new BusinessException("Numéro de téléphone invalide : " + tel);
        String email = texte(f.getEmailPersonnel());
        if (email != null && !EMAIL.matcher(email).matches()) throw new BusinessException("Adresse e-mail personnelle invalide.");
        String cin = texte(f.getCin());
        if (cin != null && dossiers.existsByCinAndIdNot(cin, dossierId))
            throw new BusinessException("Ce numéro de CIN est déjà enregistré dans un autre dossier.");

        var modifies = new ArrayList<String>();
        changer(d, "Nom", DossierEmploye::getNom, DossierEmploye::setNom, nom, modifies);
        changer(d, "Prénom", DossierEmploye::getPrenom, DossierEmploye::setPrenom, prenom, modifies);
        changer(d, "Sexe", DossierEmploye::getSexe, DossierEmploye::setSexe, sexe, modifies);
        changer(d, "Date de naissance", DossierEmploye::getDateNaissance, DossierEmploye::setDateNaissance, f.getDateNaissance(), modifies);
        changer(d, "Lieu de naissance", DossierEmploye::getLieuNaissance, DossierEmploye::setLieuNaissance, texte(f.getLieuNaissance()), modifies);
        changer(d, "Nationalité", DossierEmploye::getNationalite, DossierEmploye::setNationalite, texte(f.getNationalite()), modifies);
        changer(d, "CIN", DossierEmploye::getCin, DossierEmploye::setCin, cin, modifies);
        changer(d, "CIN délivrée le", DossierEmploye::getCinDelivreLe, DossierEmploye::setCinDelivreLe, f.getCinDelivreLe(), modifies);
        changer(d, "CIN délivrée à", DossierEmploye::getCinDelivreA, DossierEmploye::setCinDelivreA, texte(f.getCinDelivreA()), modifies);
        changer(d, "Situation familiale", DossierEmploye::getSituationFamiliale, DossierEmploye::setSituationFamiliale, situation, modifies);
        changer(d, "Nombre d’enfants", DossierEmploye::getNombreEnfants, DossierEmploye::setNombreEnfants, f.getNombreEnfants(), modifies);
        changer(d, "N° CNaPS", DossierEmploye::getNumeroCnaps, DossierEmploye::setNumeroCnaps, texte(f.getNumeroCnaps()), modifies);
        changer(d, "Adresse", DossierEmploye::getAdresse, DossierEmploye::setAdresse, texte(f.getAdresse()), modifies);
        changer(d, "Téléphone", DossierEmploye::getTelephone, DossierEmploye::setTelephone, texte(f.getTelephone()), modifies);
        changer(d, "E-mail personnel", DossierEmploye::getEmailPersonnel, DossierEmploye::setEmailPersonnel, email, modifies);
        changer(d, "Contact d’urgence", DossierEmploye::getContactUrgenceNom, DossierEmploye::setContactUrgenceNom, texte(f.getContactUrgenceNom()), modifies);
        changer(d, "Téléphone d’urgence", DossierEmploye::getContactUrgenceTelephone, DossierEmploye::setContactUrgenceTelephone, texte(f.getContactUrgenceTelephone()), modifies);
        if (!modifies.isEmpty()) {
            d.setMisAJourLe(LocalDateTime.now());
            d.setMisAJourPar(auteur);
            dossiers.save(d);
        }
        return modifies;
    }

    /** Révision de salaire : ajoute une ligne à l'historique, ne modifie jamais les précédentes. */
    @Transactional
    public Remuneration reviserSalaire(Long dossierId, LocalDate dateEffet, BigDecimal salaireBrut, String motif, String auteur) {
        var dossier = dossiers.verrouillerParId(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + dossierId));
        if (!dossier.getUtilisateur().isActif()) throw new BusinessException("Cet employé a quitté l’entreprise.");
        var contrat = contrats.findFirstByDossierIdAndStatut(dossierId, StatutContrat.EN_COURS)
                .orElseThrow(() -> new BusinessException("Enregistrez d’abord le contrat en cours de l’employé."));
        if (dateEffet == null || salaireBrut == null) throw new BusinessException("Indiquez la date d’effet et le nouveau salaire.");
        if (salaireBrut.signum() < 0) throw new BusinessException("Le salaire ne peut pas être négatif.");
        if (texte(motif) == null) throw new BusinessException("Indiquez le motif de la révision (augmentation annuelle, promotion...).");
        if (dateEffet.isBefore(contrat.getDateDebut()))
            throw new BusinessException("La date d’effet doit être postérieure au début du contrat en cours ("
                    + contrat.getDateDebut().format(JOUR) + ").");
        var derniere = remunerations.findByDossierIdOrderByDateEffetDescIdDesc(dossierId).stream().findFirst().orElse(null);
        if (derniere != null && dateEffet.isBefore(derniere.getDateEffet()))
            throw new BusinessException("La date d’effet ne peut pas précéder la dernière révision ("
                    + derniere.getDateEffet().format(JOUR) + ").");
        return remunerations.save(Remuneration.builder().dossierId(dossierId).contratId(contrat.getId())
                .dateEffet(dateEffet).salaireBrut(salaireBrut).motif(texte(motif))
                .creeLe(LocalDateTime.now()).creePar(auteur).build());
    }

    /** Dépôt d'une pièce administrative, avec sa validité. */
    @Transactional
    public void ajouterDocument(Long dossierId, TypeDocumentAdministratif type, String numero, LocalDate dateEmission,
                                LocalDate dateExpiration, MultipartFile fichier) {
        dossiers.findById(dossierId).orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + dossierId));
        if (type == null) throw new BusinessException("Choisissez le type de document.");
        if (dateEmission != null && dateEmission.isAfter(LocalDate.now()))
            throw new BusinessException("La date d’émission ne peut pas être dans le futur.");
        if (type.isAExpiration() && dateExpiration == null) {
            if (dateEmission != null && type.getValiditeMois() > 0) dateExpiration = dateEmission.plusMonths(type.getValiditeMois());
            else throw new BusinessException("Ce document a une durée de validité : indiquez sa date d’expiration.");
        }
        if (dateExpiration != null && dateEmission != null && !dateExpiration.isAfter(dateEmission))
            throw new BusinessException("La date d’expiration doit être après la date d’émission.");
        var document = documents.deposer(fichier, type.name(), type.getLibelle(), dossierId, null, null);
        document.setNumero(texte(numero));
        document.setDateEmission(dateEmission);
        document.setDateExpiration(dateExpiration);
        documentsRepository.save(document);
    }

    private static String texte(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static <T> void changer(DossierEmploye d, String libelle, Function<DossierEmploye, T> lire,
                                    java.util.function.BiConsumer<DossierEmploye, T> ecrire, T valeur, List<String> modifies) {
        if (!Objects.equals(lire.apply(d), valeur)) {
            ecrire.accept(d, valeur);
            modifies.add(libelle);
        }
    }
}
