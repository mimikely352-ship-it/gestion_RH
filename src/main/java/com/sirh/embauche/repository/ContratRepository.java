package com.sirh.embauche.repository;

import com.sirh.commun.enums.StatutContrat;
import com.sirh.embauche.entity.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat, Long> {
    /** Projection sans charger le contrat avant le verrou métier du dossier. */
    @org.springframework.data.jpa.repository.Query("select c.dossierId from Contrat c where c.id = :id")
    Optional<Long> dossierId(@org.springframework.data.repository.query.Param("id") Long id);
    List<Contrat> findByDossierIdOrderByDateDebutDescIdDesc(Long dossierId);
    Optional<Contrat> findFirstByDossierIdAndStatut(Long dossierId, StatutContrat statut);
    List<Contrat> findByDossierIdInAndStatut(Collection<Long> dossierIds, StatutContrat statut);
    List<Contrat> findByDossierIdInAndStatutIn(Collection<Long> dossierIds, Collection<StatutContrat> statuts);
    /** Premier contrat du dossier (contrat d'embauche), hors contrat annulé. */
    Optional<Contrat> findFirstByDossierIdAndStatutNotOrderByDateDebutAscIdAsc(Long dossierId, StatutContrat exclu);
    List<Contrat> findByDossierIdInAndStatutNot(Collection<Long> dossierIds, StatutContrat exclu);
    /** Contrats à venir dont la date de début est atteinte (bascule automatique). */
    List<Contrat> findByStatutAndDateDebutLessThanEqual(StatutContrat statut, LocalDate date);
    /** Contrats en cours dont la fin prévue est dépassée ou proche. */
    List<Contrat> findByStatutAndDateFinPrevueLessThanEqualOrderByDateFinPrevueAsc(StatutContrat statut, LocalDate date);
}
