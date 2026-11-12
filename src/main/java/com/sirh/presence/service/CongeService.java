package com.sirh.presence.service;
import com.sirh.presence.entity.*;
import com.sirh.presence.repository.*;
import com.sirh.commun.enums.*;
import com.sirh.commun.exception.*;
import com.sirh.candidat.service.FileStorageService;
import com.sirh.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class CongeService {
    private final DemandeCongeRepository demandes;
    private final DroitCongeRepository droits;
    private final TypeCongeRepository types;
    private final AbsenceRepository absences;
    private final PointageJourRepository pointages;
    private final PlanningService planning;
    private final AccesPresenceService acces;
    private final FileStorageService stockage;
    private final NotificationService notifications;
    private final Clock horloge;
    @Value("${app.conges.jours-calendaires:false}") private boolean joursCalendaires;
    @Value("${app.conges.validation-manager:true}") private boolean validationManager;
    private static final java.time.format.DateTimeFormatter JOUR = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public String modeDecompte() { return joursCalendaires ? "Jours calendaires" : "Jours planifiés"; }
    /** « du 10/11/2026 au 14/11/2026 », ou « du 10/11/2026 » pour une seule journée. */
    static String periode(DemandeConge c) {
        return "du " + c.getDebut().format(JOUR) + (c.getFin().equals(c.getDebut()) ? "" : " au " + c.getFin().format(JOUR));
    }
    /** Dossier concerné par une demande (retour des RH sur la fiche du salarié après une décision). */
    @Transactional(readOnly=true)
    public Long dossierDe(Long demandeId) {
        return demandes.dossierId(demandeId).orElseThrow(() -> new ResourceNotFoundException("Congé introuvable."));
    }
    @lombok.Value public static class VueDemande {
        DemandeConge demande; String salarie; boolean decisionPossible;
    }
    @Transactional(readOnly=true)
    public List<TypeConge> types() { return types.findByActifTrueOrderByNomAsc(); }
    private DemandeConge demande(Long id) {
        return demandes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Congé introuvable."));
    }
    private TypeConge type(Long id) {
        return types.findById(id).filter(TypeConge::isActif).orElseThrow(() -> new BusinessException("Type de congé indisponible."));
    }
    private void verifierDates(LocalDate debut, LocalDate fin) {
        if (debut==null || fin==null || fin.isBefore(debut) || debut.getYear()!=fin.getYear())
            throw new BusinessException("Choisissez une période dans la même année. Scindez une demande à cheval sur deux années.");
    }
    private SoldeConge solde(Long dossier, TypeConge type, int annee) {
        int droit = droits.findByDossierIdAndTypeIdAndAnnee(dossier,type.getId(),annee).map(DroitConge::getJours).orElse(0);
        int reserves=0,consommes=0;
        for (var d : demandes.findByDossierIdAndDebutLessThanEqualAndFinGreaterThanEqual(dossier,LocalDate.of(annee,12,31),LocalDate.of(annee,1,1))) {
            if (!d.getType().getId().equals(type.getId())) continue;
            if (d.getStatut()==StatutConge.ACCEPTE) consommes+=d.getJours();
            else if (d.getStatut()==StatutConge.ATTENTE_MANAGER || d.getStatut()==StatutConge.ATTENTE_RH) reserves+=d.getJours();
        }
        return new SoldeConge(type,annee,droit,reserves,consommes);
    }
    @Transactional(readOnly=true)
    public List<SoldeConge> soldes(Long id, int annee, Authentication auth) {
        acces.lire(id,auth); verifierAnnee(annee);
        return types().stream().map(t -> solde(id,t,annee)).toList();
    }
    private void verifierAnnee(int annee) { if(annee<2000 || annee>2200) throw new BusinessException("Année autorisée : 2000 à 2200."); }
    @Transactional(readOnly=true)
    public List<DemandeConge> historique(Long id, Authentication auth) {
        acces.lire(id,auth); return demandes.findByDossierIdOrderBySoumiseLeDesc(id);
    }
    private void verifierChevauchement(Long id, LocalDate debut, LocalDate fin, Long ignorer) {
        if (demandes.findByDossierIdAndDebutLessThanEqualAndFinGreaterThanEqual(id,fin,debut).stream()
                .anyMatch(d -> !Objects.equals(d.getId(),ignorer) && d.getStatut().reserve())
                || !absences.findByDossierIdAndActiveTrueAndDebutLessThanEqualAndFinGreaterThanEqual(id,fin,debut).isEmpty())
            throw new BusinessException("Cette période chevauche un congé ou une absence.");
        if (!pointages.findByDossierIdAndJourBetweenOrderByJourAsc(id,debut,fin).isEmpty())
            throw new BusinessException("Un pointage existe sur cette période.");
    }
    private String justificatif(MultipartFile fichier, boolean requis) {
        if (fichier==null || fichier.isEmpty()) {
            if (requis) throw new BusinessException("Un justificatif PDF est obligatoire.");
            return null;
        }
        stockage.verifierPdf(fichier);
        String chemin=stockage.enregistrer(fichier);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int statut) {
                    if(statut!=STATUS_COMMITTED) stockage.supprimer(chemin);
                }
            });
        }
        return chemin;
    }
    @Transactional
    public DemandeConge soumettre(Long typeId, LocalDate debut, LocalDate fin, String motif, MultipartFile fichier, Authentication auth) {
        var d=acces.verrouiller(acces.personnel(auth).getId()); acces.exigerActif(d);
        verifierDates(debut,fin); verifierAnnee(debut.getYear());
        if (debut.isBefore(LocalDate.now(horloge))) throw new BusinessException("Un congé doit être demandé avant son début.");
        if(d.getDateEntree()!=null && debut.isBefore(d.getDateEntree())) throw new BusinessException("Le congé précède l’entrée du salarié.");
        if(motif==null || motif.isBlank() || motif.length()>500) throw new BusinessException("Précisez le motif (500 caractères maximum).");
        var type=type(typeId); verifierChevauchement(d.getId(),debut,fin,null);
        int nombre=joursCalendaires ? (int)java.time.temporal.ChronoUnit.DAYS.between(debut,fin)+1
                : (int)planning.horaires(d.getId(),debut,fin).values().stream().filter(h -> !h.isRepos()).count();
        if(nombre==0) throw new BusinessException("La période ne comporte aucun jour travaillé.");
        if(type.isDeduitSolde() && solde(d.getId(),type,debut.getYear()).getDisponible()<nombre)
            throw new BusinessException("Solde de congé insuffisant.");
        Long responsable=validationManager ? acces.responsable(d) : null;
        var demande=DemandeConge.builder().dossierId(d.getId()).type(type).debut(debut).fin(fin).jours(nombre)
                .motif(motif.trim()).responsableId(responsable).statut(responsable==null ? StatutConge.ATTENTE_RH : StatutConge.ATTENTE_MANAGER)
                .soumiseLe(LocalDateTime.now(horloge)).justificatifChemin(justificatif(fichier,type.isJustificatifRequis())).build();
        demandes.save(demande);
        if(responsable!=null) notifications.notifier(responsable,"Une demande de congé attend votre validation.",TypeNotification.DEMANDE,"/conges/validations");
        else notifications.notifierRh("Une demande de congé attend votre validation.",TypeNotification.DEMANDE,"/rh/conges");
        acces.tracer(d.getId(),auth,ActionAudit.CONGE_MODIFIE,"Demande du "+debut+" au "+fin+" : "+nombre+" jour(s)");
        return demande;
    }
    private boolean peutDecider(DemandeConge demande, Authentication auth) {
        var u=acces.utilisateur(auth); var d=acces.dossier(demande.getDossierId());
        if(u.getId().equals(d.getUtilisateur().getId())) return false;
        if(demande.getStatut()==StatutConge.ATTENTE_RH) return u.getRole()==Role.RH;
        if(demande.getStatut()!=StatutConge.ATTENTE_MANAGER) return false;
        Long responsableActuel=acces.responsable(d);
        if(u.getRole()==Role.CHEF_SERVICE) return u.getId().equals(demande.getResponsableId()) && u.getId().equals(responsableActuel);
        return u.getRole()==Role.RH && (!validationManager || !Objects.equals(responsableActuel,demande.getResponsableId()));
    }
    @Transactional(readOnly=true)
    public List<VueDemande> validations(Authentication auth) {
        var u=acces.utilisateur(auth); List<DemandeConge> liste;
        if(u.getRole()==Role.RH) {
            liste=new ArrayList<>(demandes.findByStatutOrderBySoumiseLeAsc(StatutConge.ATTENTE_MANAGER));
            liste.addAll(demandes.findByStatutOrderBySoumiseLeAsc(StatutConge.ATTENTE_RH));
        } else if(u.getRole()==Role.CHEF_SERVICE) liste=demandes.findByResponsableIdAndStatutOrderBySoumiseLeAsc(u.getId(),StatutConge.ATTENTE_MANAGER);
        else throw new AccessDeniedException("Validation réservée au responsable et aux RH.");
        return liste.stream().filter(c -> u.getRole()==Role.RH || Objects.equals(acces.responsable(acces.dossier(c.getDossierId())),u.getId())).map(c -> {
            var d=acces.dossier(c.getDossierId());
            return new VueDemande(c,d.getPrenom()+" "+d.getNom(),peutDecider(c,auth));
        }).toList();
    }
    @Transactional
    public DemandeConge decider(Long id, boolean accepter, String commentaire, Authentication auth) {
        var dossierId=demandes.dossierId(id).orElseThrow(() -> new ResourceNotFoundException("Congé introuvable."));
        var d=acces.verrouiller(dossierId);
        var c=demande(id);
        if(!peutDecider(c,auth)) throw new AccessDeniedException("Cette étape ne relève pas de votre validation.");
        if(commentaire!=null && commentaire.length()>500) throw new BusinessException("Commentaire limité à 500 caractères.");
        if(!accepter && (commentaire==null || commentaire.isBlank())) throw new BusinessException("Indiquez le motif du refus.");
        if(accepter) {
            acces.exigerActif(d); verifierChevauchement(d.getId(),c.getDebut(),c.getFin(),c.getId());
            if(c.getType().isDeduitSolde() && solde(d.getId(),c.getType(),c.getDebut().getYear()).getDisponible()<0)
                throw new BusinessException("Le solde disponible ne couvre plus la demande.");
        }
        var u=acces.utilisateur(auth);
        boolean etapeManager=c.getStatut()==StatutConge.ATTENTE_MANAGER && u.getRole()==Role.CHEF_SERVICE;
        if(etapeManager) {
            c.setDecisionManagerLe(LocalDateTime.now(horloge)); c.setDecisionManagerPar(auth.getName());
            c.setCommentaireManager(commentaire == null || commentaire.isBlank() ? null : commentaire.trim());
        }
        if(accepter && etapeManager) {
            c.setStatut(StatutConge.ATTENTE_RH);
            notifications.notifierRh("Congé validé par le responsable, à examiner.",TypeNotification.DEMANDE,"/rh/conges");
        } else {
            c.setStatut(accepter ? StatutConge.ACCEPTE : StatutConge.REFUSE);
            c.setDecisionLe(LocalDateTime.now(horloge)); c.setDecisionPar(auth.getName());
            c.setCommentaire(commentaire == null || commentaire.isBlank() ? null : commentaire.trim());
            if(accepter) absences.save(Absence.builder().dossierId(d.getId()).demandeId(c.getId()).debut(c.getDebut()).fin(c.getFin())
                    .libelle(c.getType().getNom()).remuneree(c.getType().isRemunere()).active(true).enregistreePar(auth.getName()).build());
        }
        demandes.save(c);
        String message = switch (c.getStatut()) {
            case ATTENTE_RH -> "Votre demande de congé "+periode(c)+" est validée par votre responsable ; elle attend la décision des RH.";
            case ACCEPTE -> "Votre congé "+periode(c)+" est accepté.";
            case REFUSE -> "Votre demande de congé "+periode(c)+" est refusée : "+commentaire.trim();
            default -> "Votre demande de congé "+periode(c)+" a été mise à jour.";
        };
        notifications.notifier(d.getUtilisateur().getId(),message,TypeNotification.DEMANDE,"/conges");
        acces.tracer(d.getId(),auth,ActionAudit.CONGE_MODIFIE,"Décision congé "+id+" : "+c.getStatut());
        return c;
    }
    @Transactional
    public DemandeConge annuler(Long id, String motif, Authentication auth) {
        var dossierId=demandes.dossierId(id).orElseThrow(() -> new ResourceNotFoundException("Congé introuvable."));
        var d=acces.verrouiller(dossierId); var c=demande(id); var u=acces.utilisateur(auth);
        boolean rh=u.getRole()==Role.RH, proprietaire=u.getId().equals(d.getUtilisateur().getId());
        if(!rh && !proprietaire) throw new AccessDeniedException("Ce congé ne vous appartient pas.");
        if(!c.getStatut().reserve() || (c.getStatut()==StatutConge.ACCEPTE && (!rh || !c.getDebut().isAfter(LocalDate.now(horloge)))))
            throw new BusinessException("Seuls les RH peuvent annuler un congé accepté non commencé.");
        if(motif==null || motif.isBlank() || motif.length()>500) throw new BusinessException("Précisez le motif d’annulation.");
        c.setStatut(StatutConge.ANNULE); c.setAnnuleeLe(LocalDateTime.now(horloge)); c.setAnnuleePar(auth.getName());
        c.setMotifAnnulation(motif.trim());
        demandes.save(c); absences.findByDemandeId(id).ifPresent(a -> { a.setActive(false); absences.save(a); });
        // Annulation décidée par les RH : le salarié en est informé, avec le motif.
        if(!proprietaire) notifications.notifier(d.getUtilisateur().getId(),
                "Votre congé "+periode(c)+" a été annulé par les RH : "+motif.trim(),TypeNotification.DEMANDE,"/conges");
        acces.tracer(d.getId(),auth,ActionAudit.CONGE_MODIFIE,"Annulation congé "+id+" : "+motif);
        return c;
    }
    @Transactional
    public void definirDroit(Long dossier, Long typeId, int annee, int nombre, String motif, Authentication auth) {
        acces.exigerRh(auth); acces.exigerActif(acces.verrouiller(dossier)); verifierAnnee(annee); var type=type(typeId);
        if(!type.isDeduitSolde()) throw new BusinessException("Ce type ne consomme pas de droits.");
        if(nombre<0 || nombre>366 || motif==null || motif.isBlank() || motif.length()>255) throw new BusinessException("Droits : 0 à 366 jours et motif obligatoire.");
        var solde=solde(dossier,type,annee);
        if(nombre<solde.getReserves()+solde.getConsommes()) throw new BusinessException("Les droits ne couvriraient plus les congés réservés et acceptés.");
        var droit=droits.findByDossierIdAndTypeIdAndAnnee(dossier,typeId,annee).orElseGet(DroitConge::new);
        droit.setDossierId(dossier); droit.setType(type); droit.setAnnee(annee); droit.setJours(nombre); droit.setMotif(motif.trim()); droit.setModifiePar(auth.getName()); droits.save(droit);
        acces.tracer(dossier,auth,ActionAudit.DROIT_CONGE_MODIFIE,"Droits "+type.getNom()+" "+annee+" : "+solde.getDroits()+" -> "+nombre+" ; "+motif);
    }
    @Transactional
    public void ajouterType(String nom, boolean decompte, boolean remunere, boolean justificatif, Authentication auth) {
        acces.exigerRh(auth);
        if(nom==null || nom.isBlank() || nom.trim().length()>80 || types.existsByNomIgnoreCase(nom.trim())) throw new BusinessException("Nom de type unique requis (80 caractères maximum).");
        types.save(TypeConge.builder().nom(nom.trim()).deduitSolde(decompte).remunere(remunere).justificatifRequis(justificatif).actif(true).build());
    }
    @Transactional
    public void absence(Long dossier, LocalDate debut, LocalDate fin, String motif, boolean remuneree, MultipartFile fichier, Authentication auth) {
        acces.exigerRh(auth); var d=acces.verrouiller(dossier); acces.exigerActif(d); verifierDates(debut,fin); verifierAnnee(debut.getYear());
        if(d.getDateEntree()!=null && debut.isBefore(d.getDateEntree())) throw new BusinessException("L’absence précède l’entrée du salarié.");
        if(motif==null || motif.isBlank() || motif.length()>120) throw new BusinessException("Précisez le motif (120 caractères maximum).");
        verifierChevauchement(dossier,debut,fin,null);
        absences.save(Absence.builder().dossierId(dossier).debut(debut).fin(fin).libelle(motif.trim()).remuneree(remuneree).active(true)
                .justificatifChemin(justificatif(fichier,false)).enregistreePar(auth.getName()).build());
        acces.tracer(dossier,auth,ActionAudit.ABSENCE_MODIFIEE,"Absence du "+debut+" au "+fin+" : "+motif);
    }
    @Transactional
    public void annulerAbsence(Long id, String motif, Authentication auth) {
        acces.exigerRh(auth);
        var dossierId=absences.dossierId(id).orElseThrow(() -> new ResourceNotFoundException("Absence introuvable."));
        acces.verrouiller(dossierId);
        var a=absences.findById(id).orElseThrow();
        if(a.getDemandeId()!=null || !a.isActive()) throw new BusinessException("Annulez le congé associé, ou choisissez une absence active.");
        if(motif==null || motif.isBlank() || motif.length()>255) throw new BusinessException("Précisez le motif d’annulation.");
        a.setActive(false); absences.save(a); acces.tracer(a.getDossierId(),auth,ActionAudit.ABSENCE_MODIFIEE,"Annulation absence "+id+" : "+motif);
    }
    @Transactional(readOnly=true)
    public List<Absence> absences(Long dossier, Authentication auth) {
        acces.lire(dossier,auth);
        return absences.findByDossierIdAndActiveTrueAndDebutLessThanEqualAndFinGreaterThanEqual(dossier,LocalDate.of(9999,12,31),LocalDate.of(1900,1,1));
    }
    @Transactional
    public org.springframework.core.io.Resource piece(Long id, boolean absence, Authentication auth) {
        Long dossier; String chemin;
        if(absence) { var a=absences.findById(id).orElseThrow(() -> new ResourceNotFoundException("Absence introuvable.")); dossier=a.getDossierId(); chemin=a.getJustificatifChemin(); }
        else { var c=demande(id); dossier=c.getDossierId(); chemin=c.getJustificatifChemin(); }
        acces.lire(dossier,auth);
        if(chemin==null) throw new ResourceNotFoundException("Aucun justificatif.");
        var ressource=stockage.charger(chemin);
        acces.tracer(dossier,auth,ActionAudit.DOCUMENT_CONSULTE,"Justificatif "+(absence ? "absence " : "congé ")+id);
        return ressource;
    }
}
