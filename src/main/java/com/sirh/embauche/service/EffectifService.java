package com.sirh.embauche.service;
import com.sirh.commun.enums.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.demande.repository.DemandeRepository;
import com.sirh.embauche.entity.*;
import com.sirh.embauche.repository.*;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class EffectifService {
    private final DossierEmployeRepository dossiers;
    private final AffectationRepository affectations;
    private final DemandeRepository demandes;
    private final CorrectionDateEntreeRepository corrections;
    private final ContratRepository contrats;
    @Getter @RequiredArgsConstructor
    public static class Employe {
        private final String nom, email, poste, service, dateEntree;
        private final Long dossierId;
        private final List<String> historique, transferts;
        private final String matricule, role, contrat;
        /** echu | bientot | a-venir | aucun | null (contrat sans alerte). */
        private final String contratAlerte;
    }
    /** Effectif en poste : tous les salariés disposant d'un dossier (employés, chefs de service, direction). */
    @Transactional(readOnly = true)
    public List<Employe> lister() { return lister(0); }
    @Transactional(readOnly = true)
    public List<Employe> lister(int page) {
        var liste = dossiers.findByUtilisateurActifTrue(
                PageRequest.of(Math.max(0, page), 25, Sort.by("nom", "prenom", "id"))).getContent();
        if (liste.isEmpty()) return List.of();
        var ids = liste.stream().map(DossierEmploye::getId).toList();
        var contratsParDossier = new HashMap<Long, List<Contrat>>();
        contrats.findByDossierIdInAndStatutIn(ids, List.of(StatutContrat.EN_COURS, StatutContrat.A_VENIR))
                .forEach(c -> contratsParDossier.computeIfAbsent(c.getDossierId(), k -> new ArrayList<>()).add(c));
        var periodes = affectations.findByDossierIdInOrderByIdAsc(ids);
        var demandesPage = demandes.findByAuteurIdIn(liste.stream().map(e -> e.getUtilisateur().getId()).toList());
        List<Employe> resultat = new ArrayList<>();
        for (var e : liste) {
            var parcours = periodes.stream().filter(a -> a.getDossier().getId().equals(e.getId())).toList();
            var actuelle = parcours.stream().filter(Affectation::isActuelle).findFirst().orElse(null);
            var historique = new ArrayList<String>();
            parcours.forEach(a -> historique.add(a.getOrigine() + " : " + a.getPosteLibelle() + " — "
                    + (a.getServiceLibelle() == null ? "Service non renseigné" : a.getServiceLibelle())
                    + " — du " + date(a.getDateDebut()) + (a.isActuelle() ? " à aujourd’hui" : " au " + date(a.getDateFin()))));
            var mouvements = demandesPage.stream().filter(d -> d.getAuteur().getId().equals(e.getUtilisateur().getId())).toList();
            mouvements.stream().filter(d -> d.getType() == TypeDemande.FORMATION && d.getStatut() == StatutDemande.EXECUTEE)
                    .forEach(d -> historique.add("Formation réalisée : " + d.getDescription() + " — " + date(d.getDateEffet() == null && d.getDateExecution() != null ? d.getDateExecution().toLocalDate() : d.getDateEffet())));
            var attente = mouvements.stream().filter(d -> d.getStatut() != StatutDemande.REJETEE && d.getStatut() != StatutDemande.EXECUTEE)
                    .map(d -> d.getType() + " — " + d.getDescription() + " — " + d.getStatut()).toList();
            var lesContrats = contratsParDossier.getOrDefault(e.getId(), List.of());
            var enCours = lesContrats.stream().filter(c -> c.getStatut() == StatutContrat.EN_COURS).findFirst().orElse(null);
            var aVenir = lesContrats.stream().filter(c -> c.getStatut() == StatutContrat.A_VENIR).findFirst().orElse(null);
            resultat.add(new Employe(e.getPrenom() + " " + e.getNom(), e.getUtilisateur().getEmail(),
                    actuelle == null ? "Affectation à vérifier" : actuelle.getPosteLibelle(),
                    actuelle == null || actuelle.getServiceLibelle() == null ? "Service non renseigné" : actuelle.getServiceLibelle(),
                    date(e.getDateEntree()), e.getId(), historique, attente,
                    e.getMatricule(), libelleRole(e.getUtilisateur().getRole()), libelleContrat(enCours, aVenir),
                    alerteContrat(enCours, aVenir)));
        }
        return resultat;
    }
    public long compter() { return dossiers.countByUtilisateurActifTrue(); }

    private static String libelleRole(Role role) {
        if (role == null) return null;
        return switch (role) {
            case CHEF_SERVICE -> "Chef de service";
            case DIRECTION -> "Direction";
            default -> null; // employé : rôle par défaut, non affiché
        };
    }

    /** Ex. « CDD jusqu’au 12/05/2027 · puis CDD à partir du 13/05/2027 », « CDI à partir du 01/12/2026 (à venir) ». */
    static String libelleContrat(Contrat enCours, Contrat aVenir) {
        if (enCours == null && aVenir == null) return "Aucun contrat";
        String actuel = enCours == null ? null : enCours.getType().name()
                + (enCours.getDateFinPrevue() == null ? "" : " jusqu’au " + date(enCours.getDateFinPrevue()));
        String suivant = aVenir == null ? null : aVenir.getType().name() + " à partir du " + date(aVenir.getDateDebut());
        if (actuel == null) return suivant + " (à venir)";
        return suivant == null ? actuel : actuel + " · puis " + suivant;
    }

    static String alerteContrat(Contrat enCours, Contrat aVenir) {
        if (enCours == null) return aVenir == null ? "aucun" : "a-venir";
        if (aVenir != null || enCours.getDateFinPrevue() == null) return null;
        var aujourdhui = LocalDate.now();
        if (enCours.getDateFinPrevue().isBefore(aujourdhui)) return "echu";
        return enCours.getDateFinPrevue().isAfter(aujourdhui.plusDays(30)) ? null : "bientot";
    }
    private static String date(LocalDate d) {
        return d == null ? "Date non renseignée" : d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
    @Transactional
    public void renseignerEntree(Long id, LocalDate date, String auteur) {
        if (date == null || date.isAfter(LocalDate.now())) throw new BusinessException("La date d’entrée doit être passée ou aujourd’hui.");
        var dossier = dossiers.verrouillerParId(id).orElseThrow(() -> new BusinessException("Dossier employé introuvable."));
        var parcours = affectations.findByDossierIdOrderByIdAsc(id);
        if (parcours.stream().filter(a -> !"EMBAUCHE".equals(a.getOrigine())).anyMatch(a -> a.getDateDebut() != null && date.isAfter(a.getDateDebut())))
            throw new BusinessException("La date d’entrée ne peut pas être postérieure à un mouvement déjà réalisé.");
        corrections.save(CorrectionDateEntree.builder().dossierId(id).ancienneDate(dossier.getDateEntree())
                .nouvelleDate(date).auteur(auteur).horodatage(LocalDateTime.now()).build());
        dossier.setDateEntree(date);
        parcours.stream().filter(a -> "EMBAUCHE".equals(a.getOrigine())).findFirst().ifPresent(a -> {
            a.setDateDebut(date); affectations.save(a);
        });
        dossiers.save(dossier);
    }
}
