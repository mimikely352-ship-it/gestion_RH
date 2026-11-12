package com.sirh.embauche.service;

import com.sirh.commun.enums.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.embauche.entity.Contrat;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.notification.service.NotificationService;
import com.sirh.organisation.entity.Poste;
import com.sirh.organisation.entity.Section;
import com.sirh.organisation.entity.Service;
import com.sirh.organisation.repository.ServiceRepository;
import com.sirh.recrutement.entity.Candidature;
import com.sirh.recrutement.entity.OffreEmploi;
import com.sirh.recrutement.service.CandidatureService;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmbaucheServiceTest {
    final CandidatureService candidatures = mock(CandidatureService.class);
    final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    final ServiceRepository services = mock(ServiceRepository.class);
    final NotificationService notifications = mock(NotificationService.class);
    final ParcoursEmployeService parcours = mock(ParcoursEmployeService.class);
    final ContratService contrats = mock(ContratService.class);
    final AuditDossierService audit = mock(AuditDossierService.class);
    final EmbaucheService sut = new EmbaucheService(candidatures, utilisateurs, services, notifications, parcours, contrats, audit);

    final Service dev = Service.builder().id(9L).nom("Développement").build();
    final Utilisateur compte = Utilisateur.builder().id(4L).email("candidat@test.local").role(Role.CANDIDAT).actif(true).build();
    final LocalDate debut = LocalDate.now().plusDays(10), fin = LocalDate.now().plusMonths(6);

    Candidature acceptee(Poste poste) {
        var offre = OffreEmploi.builder().id(3L).titre("Développeur").poste(poste).nbPostes(1).build();
        return Candidature.builder().id(1L).statut(StatutCandidature.RETENUE).offre(offre)
                .candidat(com.sirh.candidat.entity.Candidat.builder().nom("Rabe").prenom("Zo").utilisateur(compte).build())
                .propositionStatut(StatutProposition.ACCEPTEE).propositionTypeContrat(TypeContrat.CDD)
                .propositionSalaire(new BigDecimal("1500000")).propositionDebut(debut).propositionFin(fin)
                .propositionRole(Role.CHEF_SERVICE).propositionEnvoyeePar("rh@sirh.local").build();
    }

    Poste posteDuService() {
        return Poste.builder().id(1L).intitule("Développeur Java").section(Section.builder().service(dev).build()).build();
    }

    @Test void acceptationCreeCompteDossierContratAffectationEtJournal() {
        var c = acceptee(posteDuService());
        when(utilisateurs.verrouiller(4L)).thenReturn(Optional.of(compte));
        var dossier = DossierEmploye.builder().id(7L).nom("Rabe").prenom("Zo").matricule("EMP-2026-0007").build();
        when(parcours.embaucher(eq(c), eq(dev))).thenReturn(dossier);
        when(contrats.ajouter(anyLong(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Contrat.builder().id(11L).type(TypeContrat.CDD).statut(StatutContrat.A_VENIR).build());

        assertSame(dossier, sut.embaucherALAcceptation(c, "candidat@test.local"));

        assertEquals(Role.CHEF_SERVICE, compte.getRole(), "même compte, rôle interne de la proposition");
        assertEquals(9L, compte.getService().getId());
        assertEquals(debut, c.getDateEntree());
        verify(contrats).ajouter(7L, TypeContrat.CDD, debut, fin, new BigDecimal("1500000"), null, "rh@sirh.local");
        verify(candidatures).changerStatut(1L, StatutCandidature.EMBAUCHE);
        verify(audit).journaliserDansTransaction(eq(7L), eq("candidat@test.local"), isNull(), eq(ActionAudit.EMBAUCHE),
                contains("EMP-2026-0007"));
        verify(notifications).notifierRh(contains("EMP-2026-0007"), eq(TypeNotification.EMBAUCHE), eq("/rh/employes/7"));
        verify(notifications).notifier(eq(4L), contains("EMP-2026-0007"), eq(TypeNotification.EMBAUCHE), eq("/mon-dossier"));
    }

    @Test void finalisationRefuseeSansAcceptation() {
        var c = acceptee(posteDuService());
        when(candidatures.obtenirPourModification(1L)).thenReturn(c);
        for (var statut : new StatutProposition[]{null, StatutProposition.EN_ATTENTE, StatutProposition.REFUSEE}) {
            c.setPropositionStatut(statut);
            assertThrows(BusinessException.class, () -> sut.finaliser(1L, Role.EMPLOYE, null, "rh@sirh.local"));
        }
        verifyNoInteractions(utilisateurs, parcours, contrats);
    }

    @Test void roleNonInterneRefuse() {
        var c = acceptee(posteDuService());
        c.setPropositionRole(Role.RH);
        assertThrows(BusinessException.class, () -> sut.embaucherALAcceptation(c, "candidat@test.local"));
        verifyNoInteractions(parcours, contrats);
    }

    @Test void posteSansServiceExigeLeServiceChoisiParLesRh() {
        var c = acceptee(Poste.builder().id(2L).intitule("Poste libre").build());
        assertThrows(BusinessException.class, () -> sut.serviceAccueil(c, null));
        when(services.findById(9L)).thenReturn(Optional.of(dev));
        assertSame(dev, sut.serviceAccueil(c, 9L));
        // Le service du poste prime sur un choix éventuel.
        assertSame(dev, sut.serviceAccueil(acceptee(posteDuService()), 99L));
    }
}
