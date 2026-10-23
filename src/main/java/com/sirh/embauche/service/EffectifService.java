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
    @Getter @RequiredArgsConstructor
    public static class Employe {
        private final String nom, email, poste, service, dateEntree;
        private final Long dossierId;
        private final List<String> historique, transferts;
    }
    @Transactional(readOnly = true)
    public List<Employe> lister() { return lister(0); }
    @Transactional(readOnly = true)
    public List<Employe> lister(int page) {
        var liste = dossiers.findByUtilisateurRoleAndUtilisateurActifTrue(Role.EMPLOYE,
                PageRequest.of(Math.max(0, page), 25, Sort.by("nom", "prenom", "id"))).getContent();
        if (liste.isEmpty()) return List.of();
        var periodes = affectations.findByDossierIdInOrderByIdAsc(liste.stream().map(DossierEmploye::getId).toList());
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
            resultat.add(new Employe(e.getPrenom() + " " + e.getNom(), e.getUtilisateur().getEmail(),
                    actuelle == null ? "Affectation à vérifier" : actuelle.getPosteLibelle(),
                    actuelle == null || actuelle.getServiceLibelle() == null ? "Service non renseigné" : actuelle.getServiceLibelle(),
                    date(e.getDateEntree()), e.getId(), historique, attente));
        }
        return resultat;
    }
    public long compter() { return dossiers.countByUtilisateurRoleAndUtilisateurActifTrue(Role.EMPLOYE); }
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
