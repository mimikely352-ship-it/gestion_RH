package com.sirh.embauche.service;

import com.sirh.commun.enums.*;
import com.sirh.demande.entity.Demande;
import com.sirh.demande.repository.DemandeRepository;
import com.sirh.organisation.entity.*;
import com.sirh.recrutement.entity.*;
import com.sirh.recrutement.repository.CandidatureRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AffectationAutomatiqueServiceTest {
    @Test void embaucheReprendLeServiceDuPosteSansSaisieRh() {
        var gestion = mock(com.sirh.recrutement.service.CandidatureService.class);
        var notifications = mock(com.sirh.notification.service.NotificationService.class);
        var employe = Utilisateur.builder().id(1L).role(Role.CANDIDAT).build();
        var transport = Service.builder().id(2L).build();
        var c = Candidature.builder().id(3L).statut(StatutCandidature.RETENUE)
                .candidat(com.sirh.candidat.entity.Candidat.builder().utilisateur(employe).build())
                .offre(OffreEmploi.builder().poste(Poste.builder()
                        .section(Section.builder().service(transport).build()).build()).build()).build();
        c.setPropositionStatut(StatutProposition.ACCEPTEE);
        c.setPropositionDebut(java.time.LocalDate.now());
        c.setPropositionTypeContrat(TypeContrat.CDI);
        c.setPropositionSalaire(new java.math.BigDecimal("1000.00"));
        when(gestion.obtenirPourModification(3L)).thenReturn(c);
        when(utilisateurs.verrouiller(1L)).thenReturn(java.util.Optional.of(employe));
        when(gestion.changerStatut(3L, StatutCandidature.EMBAUCHE)).thenReturn(c);
        var parcours = mock(ParcoursEmployeService.class);
        when(parcours.embaucher(c, transport)).thenReturn(com.sirh.embauche.entity.DossierEmploye.builder().id(8L).build());
        var contrats = mock(ContratService.class);
        when(contrats.ajouter(anyLong(), any(), any(), any(), any(), any(), any())).thenReturn(
                com.sirh.embauche.entity.Contrat.builder().id(4L).type(TypeContrat.CDI).statut(StatutContrat.EN_COURS).build());
        new EmbaucheService(gestion, utilisateurs, mock(com.sirh.organisation.repository.ServiceRepository.class), notifications,
                parcours, contrats, mock(AuditDossierService.class))
                .finaliser(3L, Role.EMPLOYE, null, "rh@sirh.local");
        assertSame(transport, employe.getService());
        assertEquals(Role.EMPLOYE, employe.getRole());
        assertNotNull(c.getDateEntree());
        verify(utilisateurs).save(employe);
        // Le contrat est enregistré au nom du compte RH qui a réalisé l'embauche (plus « EMBAUCHE »).
        verify(contrats).ajouter(8L, TypeContrat.CDI, c.getPropositionDebut(), null, c.getPropositionSalaire(), null, "rh@sirh.local");
    }

    private final com.sirh.embauche.repository.DossierEmployeRepository dossiers = mock(com.sirh.embauche.repository.DossierEmployeRepository.class);
    private final com.sirh.embauche.repository.AffectationRepository affectations = mock(com.sirh.embauche.repository.AffectationRepository.class);
    private final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    private final AffectationAutomatiqueService sut = new AffectationAutomatiqueService(dossiers, affectations, utilisateurs);

    @Test void serviceIssuDuSnapshotMemeSiPosteChangeDeSection() {
        var employe = Utilisateur.builder().id(1L).role(Role.EMPLOYE).build();
        var transport = Service.builder().id(2L).build();
        when(utilisateurs.verrouiller(1L)).thenReturn(java.util.Optional.of(employe));
        when(dossiers.findByUtilisateurId(1L)).thenReturn(java.util.Optional.of(com.sirh.embauche.entity.DossierEmploye.builder().id(3L).build()));
        when(affectations.findByDossierIdAndActuelleTrue(3L)).thenReturn(java.util.Optional.of(com.sirh.embauche.entity.Affectation.builder().service(transport).build()));
        sut.completer(employe);
        assertSame(transport, employe.getService());
        verify(utilisateurs).save(employe);
    }
    @Test void neDevinePasUnServiceSansDossier() {
        var employe = Utilisateur.builder().id(1L).role(Role.EMPLOYE).build();
        when(utilisateurs.verrouiller(1L)).thenReturn(java.util.Optional.of(employe));
        sut.completer(employe);
        assertNull(employe.getService()); verify(utilisateurs,never()).save(any());
    }
    @Test void neDevinePasUneAffectationAmbigue() {
        var employe = Utilisateur.builder().id(1L).role(Role.EMPLOYE).build();
        when(utilisateurs.verrouiller(1L)).thenReturn(java.util.Optional.of(employe));
        when(dossiers.findByUtilisateurId(1L)).thenReturn(java.util.Optional.of(com.sirh.embauche.entity.DossierEmploye.builder().id(3L).build()));
        sut.completer(employe);
        assertNull(employe.getService()); verify(utilisateurs,never()).save(any());
    }
    @Test void neModifiePasLesComptesRh() {
        sut.completer(Utilisateur.builder().role(Role.RH).build());
        verifyNoInteractions(utilisateurs, dossiers, affectations);
    }
}
