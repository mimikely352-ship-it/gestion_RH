package com.sirh.embauche.service;

import com.sirh.candidat.entity.Document;
import com.sirh.candidat.repository.CandidatRepository;
import com.sirh.candidat.repository.DocumentRepository;
import com.sirh.candidat.repository.ExperienceRepository;
import com.sirh.commun.enums.*;
import com.sirh.commun.exception.ResourceNotFoundException;
import com.sirh.demande.repository.DemandeRepository;
import com.sirh.embauche.entity.*;
import com.sirh.embauche.repository.*;
import com.sirh.embauche.service.HabilitationService.Habilitation;
import com.sirh.recrutement.repository.CandidatureRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dossier salarié, référentiel central : identité, coordonnées, situation professionnelle, contrats,
 * rémunération, carrière, documents administratifs et journal d'audit. Les rubriques non couvertes
 * par l'habilitation du lecteur ne sont pas chargées dans la vue (confidentialité).
 */
@Service
@RequiredArgsConstructor
public class FicheEmployeService {
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final DossierEmployeRepository dossiers;
    private final AffectationRepository affectations;
    private final CorrectionDateEntreeRepository corrections;
    private final DemandeRepository demandes;
    private final CandidatRepository candidats;
    private final ExperienceRepository experiences;
    private final DocumentRepository documents;
    private final CandidatureRepository candidatures;
    private final ContratRepository contrats;
    private final DepartRepository departs;
    private final RemunerationRepository remunerations;
    private final AuditDossierRepository journal;
    private final OnboardingService onboarding;

    public record LigneParcours(String origine, String poste, String service, String debut, String fin, boolean actuelle) {
        public String getOrigine() { return origine; }
        public String getPoste() { return poste; }
        public String getService() { return service; }
        public String getDebut() { return debut; }
        public String getFin() { return fin; }
        public boolean isActuelle() { return actuelle; }
    }
    public record LigneDemande(String type, String description, String statut, String date) {
        public String getType() { return type; }
        public String getDescription() { return description; }
        public String getStatut() { return statut; }
        public String getDate() { return date; }
    }
    public record LigneCorrection(String ancienne, String nouvelle, String auteur, String horodatage) {
        public String getAncienne() { return ancienne; }
        public String getNouvelle() { return nouvelle; }
        public String getAuteur() { return auteur; }
        public String getHorodatage() { return horodatage; }
    }
    public record LigneExperience(String intitule, String entreprise, String periode, String description) {
        public String getIntitule() { return intitule; }
        public String getEntreprise() { return entreprise; }
        public String getPeriode() { return periode; }
        public String getDescription() { return description; }
    }
    public record LigneDocument(Long id, String libelle, String nomFichier, String date) {
        public Long getId() { return id; }
        public String getLibelle() { return libelle; }
        public String getNomFichier() { return nomFichier; }
        public String getDate() { return date; }
    }
    /**
     * @param version    numéro de version du document pour son type (1 = premier dépôt) ; 0 pour les pièces
     *                   à plusieurs exemplaires (diplômes, autres), qui ne se remplacent pas
     * @param remplaceLe date du dépôt de la version suivante, null pour la version en vigueur
     */
    public record LigneDocumentAdministratif(Long id, String libelle, String numero, String emission, String expiration,
                                            ValiditeDocument validite, String nomFichier, boolean remplace,
                                            int version, String remplaceLe, String depose) {
        public Long getId() { return id; }
        public String getLibelle() { return libelle; }
        public String getNumero() { return numero; }
        public String getEmission() { return emission; }
        public String getExpiration() { return expiration; }
        public String getValidite() { return validite.name(); }
        public String getValiditeLibelle() { return remplace ? "Remplacé" : validite.getLibelle(); }
        public String getNomFichier() { return nomFichier; }
        public boolean isRemplace() { return remplace; }
        public int getVersion() { return version; }
        public String getRemplaceLe() { return remplaceLe; }
        public String getDepose() { return depose; }
    }
    public record LigneRemuneration(String dateEffet, String salaire, String motif, String saisie, boolean actuelle) {
        public String getDateEffet() { return dateEffet; }
        public String getSalaire() { return salaire; }
        public String getMotif() { return motif; }
        public String getSaisie() { return saisie; }
        public boolean isActuelle() { return actuelle; }
    }
    public record LigneAudit(String horodatage, String acteur, String role, String action, String detail) {
        public String getHorodatage() { return horodatage; }
        public String getActeur() { return acteur; }
        public String getRole() { return role; }
        public String getAction() { return action; }
        public String getDetail() { return detail; }
    }
    /**
     * @param precedent « Renouvelle le CDD du 12/11/2026 » si le contrat en remplace un autre
     * @param suivant   « Remplacé par le CDD du 13/05/2027 » si un contrat lui succède
     */
    public record LigneContrat(Long id, String type, String libelleType, String debut, String finPrevue, String finEffective,
                               String statut, String salaire, String signeLe, Long documentId, String motifFin, boolean echu,
                               String precedent, String suivant) {
        public String getPrecedent() { return precedent; }
        public String getSuivant() { return suivant; }
        public boolean isAVenir() { return "A_VENIR".equals(statut); }
        public Long getId() { return id; }
        public String getType() { return type; }
        public String getLibelleType() { return libelleType; }
        public String getDebut() { return debut; }
        public String getFinPrevue() { return finPrevue; }
        public String getFinEffective() { return finEffective; }
        public String getStatut() { return statut; }
        public String getSalaire() { return salaire; }
        public String getSigneLe() { return signeLe; }
        public Long getDocumentId() { return documentId; }
        public String getMotifFin() { return motifFin; }
        public boolean isEchu() { return echu; }
        public boolean isEnCours() { return "EN_COURS".equals(statut); }
    }
    public record LignePiece(String code, String libelle, boolean obligatoire, List<LigneDocument> fichiers) {
        public String getCode() { return code; }
        public String getLibelle() { return libelle; }
        public boolean isObligatoire() { return obligatoire; }
        public List<LigneDocument> getFichiers() { return fichiers; }
        public boolean isDeposee() { return !fichiers.isEmpty(); }
    }
    public record LigneDepart(Long id, String type, String libelleType, String motif, String description,
                              String dateNotification, String dateEffet, String statut, String cree, String cloture,
                              List<LignePiece> pieces, boolean effetAtteint) {
        public Long getId() { return id; }
        public String getType() { return type; }
        public String getLibelleType() { return libelleType; }
        public String getMotif() { return motif; }
        public String getDescription() { return description; }
        public String getDateNotification() { return dateNotification; }
        public String getDateEffet() { return dateEffet; }
        public String getStatut() { return statut; }
        public String getCree() { return cree; }
        public String getCloture() { return cloture; }
        public List<LignePiece> getPieces() { return pieces; }
        public boolean isEffetAtteint() { return effetAtteint; }
        public boolean isEnCours() { return "EN_COURS".equals(statut); }
        public boolean isPret() {
            return effetAtteint && pieces.stream().allMatch(p -> !p.obligatoire() || p.isDeposee());
        }
    }

    @Getter @Builder
    public static class Fiche {
        private final Habilitation habilitation;
        private final Long dossierId;
        private final String matricule, nom, prenom, nomComplet, email, role;
        private final boolean actif;
        private final String compteCreeLe, misAJour;
        // identité / coordonnées (null si non habilité)
        private final String sexe, dateNaissance, lieuNaissance, nationalite, cin, cinDelivrance, situationFamiliale,
                nombreEnfants, numeroCnaps, adresse, telephone, emailPersonnel, contactUrgence;
        // valeurs brutes pour le formulaire RH
        private final DossierEmploye saisie;
        private final String poste, service, dateEntree, anciennete;
        private final Long candidatureId;
        private final String offreRecrutement;
        private final OnboardingService.Vue onboarding;
        private final LigneContrat contratEnCours;
        /** Contrat saisi à l'avance (renouvellement, embauche avec entrée future), pas encore en vigueur. */
        private final LigneContrat contratAVenir;
        private final List<LigneContrat> contrats;
        private final String salaireActuel;
        private final List<LigneRemuneration> remunerations;
        private final LigneDepart departEnCours;
        private final LigneDepart departCloture;
        private final List<LigneDepart> departs;
        private final List<LigneParcours> parcours;
        private final List<LigneDemande> demandes;
        private final List<LigneCorrection> corrections;
        private final List<String> diplomes;
        private final List<LigneExperience> experiences;
        private final List<LigneDocumentAdministratif> documentsAdministratifs;
        private final List<String> piecesAdministrativesManquantes;
        private final List<LigneDocument> autresDocuments;
        private final List<LigneAudit> audit;
    }

    @Transactional(readOnly = true)
    public DossierEmploye dossier(Long dossierId) {
        var d = dossiers.findById(dossierId).orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + dossierId));
        d.getUtilisateur().getRole(); // charge le titulaire pour le contrôle d'habilitation
        if (d.getUtilisateur().getService() != null && d.getUtilisateur().getService().getChefService() != null)
            d.getUtilisateur().getService().getChefService().getId();
        return d;
    }

    @Transactional(readOnly = true)
    public Fiche obtenir(Long dossierId, Habilitation h) {
        if (h == null || !h.lecture()) throw new org.springframework.security.access.AccessDeniedException("Accès refusé à ce dossier.");
        DossierEmploye dossier = dossiers.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + dossierId));
        var utilisateur = dossier.getUtilisateur();
        var aujourdhui = LocalDate.now();
        var periodes = affectations.findByDossierIdOrderByIdAsc(dossierId);
        var actuelle = periodes.stream().filter(Affectation::isActuelle).findFirst().orElse(null);
        var derniere = actuelle != null ? actuelle : periodes.isEmpty() ? null : periodes.get(periodes.size() - 1);
        var candidat = candidats.findByUtilisateurId(utilisateur.getId()).orElse(null);
        var recrutement = candidatures.findByCandidatUtilisateurIdAndStatut(utilisateur.getId(), StatutCandidature.EMBAUCHE)
                .stream().findFirst().orElse(null);

        var docsEmploye = h.documents() ? documents.findByDossierIdOrderByDateUploadDesc(dossierId) : List.<Document>of();
        var listeContrats = contrats.findByDossierIdOrderByDateDebutDescIdDesc(dossierId);
        var lignesContrats = listeContrats.stream()
                .map(c -> contrat(c, listeContrats, docsEmploye, h.salaire())).toList();
        var lignesDeparts = departs.findByDossierIdOrderByIdDesc(dossierId).stream()
                .filter(d -> d.getStatut() != StatutDepart.ANNULE)
                .map(d -> depart(d, docsEmploye)).toList();

        // ----- Rémunération -----
        List<LigneRemuneration> lignesRemu = List.of();
        String salaireActuel = null;
        if (h.salaire()) {
            var historique = remunerations.findByDossierIdOrderByDateEffetDescIdDesc(dossierId);
            var enVigueur = historique.stream().filter(r -> !r.getDateEffet().isAfter(aujourdhui)).findFirst().orElse(null);
            salaireActuel = enVigueur == null ? null : montant(enVigueur.getSalaireBrut());
            lignesRemu = historique.stream().map(r -> new LigneRemuneration(date(r.getDateEffet()), montant(r.getSalaireBrut()),
                    r.getMotif(), r.getCreeLe().format(HORODATAGE) + " par " + r.getCreePar(), r == enVigueur)).toList();
        }

        // ----- Documents administratifs : versions -----
        // Pièce unique (CIN, certificat...) : chaque dépôt est une nouvelle version qui remplace la précédente.
        // Pièces à plusieurs exemplaires (diplômes, autres) : les dépôts s'ajoutent sans se remplacer.
        var administratifs = docsEmploye.stream()
                .filter(d -> d.getContratId() == null && d.getDepartId() == null && TypeDocumentAdministratif.depuis(d.getType()) != null)
                .toList();
        var plusRecentParType = new HashMap<String, Document>();
        var versions = new HashMap<Long, Integer>();
        var remplaceLe = new HashMap<Long, String>();
        administratifs.stream().collect(Collectors.groupingBy(Document::getType)).forEach((code, depots) -> {
            var ordre = depots.stream().sorted(ORDRE_DEPOT).toList();
            plusRecentParType.put(code, ordre.get(ordre.size() - 1));
            if (TypeDocumentAdministratif.depuis(code).isPlusieursExemplaires()) return;
            for (int i = 0; i < ordre.size(); i++) {
                versions.put(ordre.get(i).getId(), i + 1);
                if (i + 1 < ordre.size()) remplaceLe.put(ordre.get(i).getId(), horodatage(ordre.get(i + 1)));
            }
        });
        var lignesAdmin = administratifs.stream().map(d -> new LigneDocumentAdministratif(d.getId(),
                TypeDocumentAdministratif.depuis(d.getType()).getLibelle(), d.getNumero(),
                d.getDateEmission() == null ? "—" : date(d.getDateEmission()),
                d.getDateExpiration() == null ? "—" : date(d.getDateExpiration()),
                ValiditeDocument.de(d.getDateExpiration(), aujourdhui), d.getNomFichier(),
                remplaceLe.containsKey(d.getId()), versions.getOrDefault(d.getId(), 0), remplaceLe.get(d.getId()),
                horodatage(d))).toList();
        List<String> manquantes = !h.documents() ? List.of() : Arrays.stream(TypeDocumentAdministratif.values())
                .filter(TypeDocumentAdministratif::isObligatoire)
                .filter(t -> { var d = plusRecentParType.get(t.name());
                    return d == null || ValiditeDocument.de(d.getDateExpiration(), aujourdhui) == ValiditeDocument.EXPIRE; })
                .map(TypeDocumentAdministratif::getLibelle).toList();
        // Exemplaires signés déposés plusieurs fois pour un même contrat : versions numérotées.
        var suffixes = new HashMap<Long, String>();
        docsEmploye.stream().filter(d -> d.getContratId() != null).collect(Collectors.groupingBy(Document::getContratId))
                .forEach((contratId, depots) -> {
                    if (depots.size() < 2) return;
                    var ordre = depots.stream().sorted(ORDRE_DEPOT).toList();
                    for (int i = 0; i < ordre.size(); i++)
                        suffixes.put(ordre.get(i).getId(), " — version " + (i + 1) + (i + 1 < ordre.size()
                                ? ", remplacée le " + horodatage(ordre.get(i + 1)) : " (en vigueur)"));
                });
        var autres = new ArrayList<LigneDocument>();
        docsEmploye.stream().filter(d -> !administratifs.contains(d))
                .forEach(d -> autres.add(document(d, suffixes.getOrDefault(d.getId(), ""))));

        // ----- Carrière -----
        var parcours = periodes.stream().map(a -> new LigneParcours(origine(a.getOrigine()), a.getPosteLibelle(),
                a.getServiceLibelle() == null ? "Service non renseigné" : a.getServiceLibelle(),
                date(a.getDateDebut()), a.isActuelle() ? "Aujourd’hui" : date(a.getDateFin()), a.isActuelle())).toList();
        var lignesDemandes = demandes.findByAuteurId(utilisateur.getId()).stream()
                .sorted(Comparator.comparing(d -> d.getDateCreation() == null ? LocalDateTime.MIN : d.getDateCreation(),
                        Comparator.reverseOrder()))
                .map(d -> new LigneDemande(String.valueOf(d.getType()), d.getDescription(), String.valueOf(d.getStatut()),
                        d.getDateCreation() == null ? "Date non renseignée" : d.getDateCreation().format(JOUR)))
                .toList();
        var lignesCorrections = corrections.findByDossierIdOrderByHorodatageDesc(dossierId).stream()
                .map(c -> new LigneCorrection(date(c.getAncienneDate()), date(c.getNouvelleDate()), c.getAuteur(),
                        c.getHorodatage().format(HORODATAGE)))
                .toList();

        List<String> diplomes = List.of();
        List<LigneExperience> lignesExperiences = List.of();
        if (candidat != null) {
            diplomes = candidat.getDiplomes().stream().map(d -> d.getLibelle()).sorted().toList();
            lignesExperiences = experiences.findByCandidatId(candidat.getId()).stream()
                    .map(e -> new LigneExperience(e.getIntitule(), e.getEntreprise(),
                            date(e.getDateDebut()) + " → " + (e.getDateFin() == null ? "en cours" : date(e.getDateFin())),
                            e.getDescription()))
                    .toList();
            if (h.documents())
                documents.findByCandidatIdOrderByDateUploadDesc(candidat.getId()).forEach(d -> autres.add(document(d)));
        }

        List<LigneAudit> lignesAudit = !h.audit() ? List.of() : journal.findTop100ByDossierIdOrderByHorodatageDescIdDesc(dossierId)
                .stream().map(a -> new LigneAudit(a.getHorodatage().format(HORODATAGE), a.getActeur(),
                        a.getRole(), a.getAction().getLibelle(), a.getDetail())).toList();

        boolean perso = h.donneesPersonnelles();
        var b = Fiche.builder()
                .habilitation(h)
                .dossierId(dossierId).matricule(dossier.getMatricule())
                .nom(dossier.getNom()).prenom(dossier.getPrenom())
                .nomComplet(dossier.getPrenom() + " " + dossier.getNom())
                .email(utilisateur.getEmail())
                .role(String.valueOf(utilisateur.getRole()))
                .actif(utilisateur.isActif())
                .compteCreeLe(utilisateur.getDateCreation() == null ? "Date non renseignée" : utilisateur.getDateCreation().format(HORODATAGE))
                .misAJour(dossier.getMisAJourLe() == null ? null : dossier.getMisAJourLe().format(HORODATAGE) + " par " + dossier.getMisAJourPar())
                .poste(derniere == null ? "Affectation à vérifier" : derniere.getPosteLibelle())
                .service(derniere == null || derniere.getServiceLibelle() == null ? "Service non renseigné" : derniere.getServiceLibelle())
                .dateEntree(date(dossier.getDateEntree()))
                .anciennete(anciennete(dossier.getDateEntree(), utilisateur.isActif() || derniere == null ? null : derniere.getDateFin()))
                .candidatureId(recrutement == null || !h.modification() ? null : recrutement.getId())
                .offreRecrutement(recrutement == null || recrutement.getOffre() == null ? null : recrutement.getOffre().getTitre())
                .onboarding(onboarding.vue(dossier))
                .contrats(lignesContrats)
                .contratEnCours(lignesContrats.stream().filter(LigneContrat::isEnCours).findFirst().orElse(null))
                .contratAVenir(lignesContrats.stream().filter(LigneContrat::isAVenir).findFirst().orElse(null))
                .salaireActuel(salaireActuel).remunerations(lignesRemu)
                .departs(h.depart() ? lignesDeparts : List.of())
                .departEnCours(h.depart() ? lignesDeparts.stream().filter(LigneDepart::isEnCours).findFirst().orElse(null) : null)
                .departCloture(lignesDeparts.stream().filter(d -> "CLOTURE".equals(d.statut())).findFirst()
                        .map(d -> h.depart() ? d : new LigneDepart(d.id(), d.type(), d.libelleType(), null, null, null,
                                d.dateEffet(), d.statut(), null, null, List.of(), true)).orElse(null))
                .parcours(parcours).demandes(lignesDemandes).corrections(lignesCorrections)
                .diplomes(diplomes).experiences(lignesExperiences)
                .documentsAdministratifs(lignesAdmin).piecesAdministrativesManquantes(manquantes).autresDocuments(autres)
                .audit(lignesAudit);
        if (perso) {
            b.saisie(h.modification() ? dossier : null)
             .sexe("F".equals(dossier.getSexe()) ? "Féminin" : "M".equals(dossier.getSexe()) ? "Masculin" : null)
             .dateNaissance(dossier.getDateNaissance() == null ? null : date(dossier.getDateNaissance())
                     + " (" + Period.between(dossier.getDateNaissance(), aujourdhui).getYears() + " ans)")
             .lieuNaissance(dossier.getLieuNaissance()).nationalite(dossier.getNationalite()).cin(dossier.getCin())
             .cinDelivrance(dossier.getCinDelivreLe() == null && dossier.getCinDelivreA() == null ? null
                     : "le " + (dossier.getCinDelivreLe() == null ? "?" : date(dossier.getCinDelivreLe()))
                       + (dossier.getCinDelivreA() == null ? "" : " à " + dossier.getCinDelivreA()))
             .situationFamiliale(situation(dossier.getSituationFamiliale()))
             .nombreEnfants(dossier.getNombreEnfants() == null ? null : String.valueOf(dossier.getNombreEnfants()))
             .numeroCnaps(dossier.getNumeroCnaps()).adresse(dossier.getAdresse()).telephone(dossier.getTelephone())
             .emailPersonnel(dossier.getEmailPersonnel())
             .contactUrgence(dossier.getContactUrgenceNom() == null ? null : dossier.getContactUrgenceNom()
                     + (dossier.getContactUrgenceTelephone() == null ? "" : " — " + dossier.getContactUrgenceTelephone()));
        }
        return b.build();
    }

    /** Pièces administratives expirées ou à renouveler (dernière version de chaque type), pour les employés actifs. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> documentsARenouveler() {
        var aujourdhui = LocalDate.now();
        var candidats = documents.findByDossierIdIsNotNullAndDateExpirationLessThanEqualOrderByDateExpirationAsc(
                aujourdhui.plusDays(ValiditeDocument.PREAVIS_JOURS));
        if (candidats.isEmpty()) return List.of();
        var ids = candidats.stream().map(Document::getDossierId).collect(Collectors.toSet());
        var actifs = dossiers.findAllById(ids).stream().filter(d -> d.getUtilisateur().isActif())
                .collect(Collectors.toMap(DossierEmploye::getId, d -> d));
        var resultat = new ArrayList<Map<String, Object>>();
        for (var d : candidats) {
            var type = TypeDocumentAdministratif.depuis(d.getType());
            var dossier = actifs.get(d.getDossierId());
            if (type == null || dossier == null || d.getContratId() != null || d.getDepartId() != null) continue;
            boolean remplace = !type.isPlusieursExemplaires() && documents.findByDossierIdOrderByDateUploadDesc(d.getDossierId())
                    .stream().anyMatch(x -> d.getType().equals(x.getType()) && x.getContratId() == null
                            && x.getDepartId() == null && ORDRE_DEPOT.compare(x, d) > 0);
            if (remplace) continue;
            resultat.add(Map.of("dossierId", dossier.getId(), "nom", dossier.getPrenom() + " " + dossier.getNom(),
                    "document", type.getLibelle(), "expiration", date(d.getDateExpiration()),
                    "validite", ValiditeDocument.de(d.getDateExpiration(), aujourdhui).getLibelle()));
        }
        return resultat;
    }

    /** Ordre de dépôt des documents : date d'envoi puis identifiant (deux dépôts dans la même seconde). */
    static final Comparator<Document> ORDRE_DEPOT = Comparator
            .comparing(Document::getDateUpload, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(Document::getId, Comparator.nullsFirst(Comparator.naturalOrder()));

    private static LigneContrat contrat(Contrat c, List<Contrat> tous, List<Document> docs, boolean salaire) {
        // Exemplaire signé en vigueur : le dernier déposé pour ce contrat.
        Long documentId = docs.stream().filter(d -> c.getId().equals(d.getContratId())).max(ORDRE_DEPOT)
                .map(Document::getId).orElse(null);
        boolean echu = c.getStatut() == StatutContrat.EN_COURS && c.getDateFinPrevue() != null
                && c.getDateFinPrevue().isBefore(LocalDate.now());
        String precedent = c.getContratPrecedentId() == null ? null : tous.stream()
                .filter(p -> p.getId().equals(c.getContratPrecedentId())).findFirst()
                .map(p -> "Renouvelle le " + p.getType().name() + " du " + date(p.getDateDebut())).orElse(null);
        String suivant = tous.stream().filter(s -> c.getId().equals(s.getContratPrecedentId())
                        && s.getStatut() != StatutContrat.ANNULE).findFirst()
                .map(s -> "Remplacé par le " + s.getType().name() + " du " + date(s.getDateDebut())
                        + (s.getStatut() == StatutContrat.A_VENIR ? " (à venir)" : "")).orElse(null);
        return new LigneContrat(c.getId(), c.getType().name(), c.getType().getLibelle(), date(c.getDateDebut()),
                c.getDateFinPrevue() == null ? "—" : date(c.getDateFinPrevue()),
                c.getDateFinEffective() == null ? null : date(c.getDateFinEffective()),
                c.getStatut().name(), salaire ? (c.getSalaireBrut() == null ? "Non renseigné" : montant(c.getSalaireBrut())) : "Confidentiel",
                c.getDateSignature() == null ? null : date(c.getDateSignature()), documentId, c.getMotifFin(), echu,
                precedent, suivant);
    }

    private static LigneDepart depart(Depart d, List<Document> docs) {
        var pieces = PieceDepart.pour(d.getType()).stream().map(p -> new LignePiece(p.name(), p.getLibelle(), p.isObligatoire(),
                docs.stream().filter(doc -> d.getId().equals(doc.getDepartId()) && p.name().equals(doc.getType()))
                        .map(FicheEmployeService::document).toList())).toList();
        return new LigneDepart(d.getId(), d.getType().name(), d.getType().getLibelle(),
                d.getMotif() == null ? null : MotifLicenciement.valueOf(d.getMotif()).getLibelle(), d.getDescription(),
                date(d.getDateNotification()), date(d.getDateEffet()), d.getStatut().name(),
                d.getCreeLe().format(HORODATAGE) + " par " + d.getCreePar(),
                d.getClotureLe() == null ? null : d.getClotureLe().format(HORODATAGE) + " par " + d.getCloturePar(),
                pieces, !d.getDateEffet().isAfter(LocalDate.now()));
    }

    private static LigneDocument document(Document d) {
        return document(d, "");
    }

    /** @param suffixe précision ajoutée au libellé (version d'un exemplaire signé...) */
    private static LigneDocument document(Document d, String suffixe) {
        String libelle = d.getLibelle() != null && !d.getLibelle().isBlank() ? d.getLibelle() : d.getType();
        return new LigneDocument(d.getId(), libelle + suffixe, d.getNomFichier(), d.getDateUpload() == null ? "" : d.getDateUpload().format(JOUR));
    }

    private static String horodatage(Document d) {
        return d.getDateUpload() == null ? "date inconnue" : d.getDateUpload().format(HORODATAGE);
    }

    private static String situation(String code) {
        if (code == null) return null;
        return switch (code) {
            case "CELIBATAIRE" -> "Célibataire";
            case "MARIE" -> "Marié(e)";
            case "UNION_LIBRE" -> "Union libre";
            case "DIVORCE" -> "Divorcé(e)";
            case "VEUF" -> "Veuf / veuve";
            default -> code;
        };
    }

    private static String montant(BigDecimal montant) {
        var format = NumberFormat.getNumberInstance(Locale.FRANCE);
        format.setMaximumFractionDigits(2);
        return format.format(montant) + " (brut mensuel)";
    }

    private static String origine(String origine) {
        if (origine == null) return "Mouvement";
        return switch (origine) {
            case "EMBAUCHE" -> "Embauche";
            case "PROMOTION" -> "Promotion";
            case "TRANSFERT" -> "Transfert";
            case "MUTATION" -> "Mutation";
            default -> origine.charAt(0) + origine.substring(1).toLowerCase().replace('_', ' ');
        };
    }

    private static String anciennete(LocalDate entree, LocalDate sortie) {
        if (entree == null) return "Inconnue";
        var p = Period.between(entree, sortie == null ? LocalDate.now() : sortie);
        if (p.isNegative()) return "Entrée prévue";
        if (p.getYears() == 0 && p.getMonths() == 0) return p.getDays() + " jour(s)";
        return (p.getYears() > 0 ? p.getYears() + " an(s) " : "") + p.getMonths() + " mois";
    }

    private static String date(LocalDate d) {
        return d == null ? "Date non renseignée" : d.format(JOUR);
    }
}
