package com.sirh.embauche.service;

import com.sirh.commun.enums.Role;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.embauche.service.HabilitationService.Habilitation;
import com.sirh.organisation.entity.Service;
import com.sirh.utilisateur.entity.Utilisateur;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HabilitationServiceTest {
    final Utilisateur chef = Utilisateur.builder().id(10L).role(Role.CHEF_SERVICE).actif(true).build();
    final Utilisateur autreChef = Utilisateur.builder().id(11L).role(Role.CHEF_SERVICE).actif(true).build();
    final Service transport = Service.builder().id(1L).chefService(chef).build();
    final Utilisateur employe = Utilisateur.builder().id(2L).role(Role.EMPLOYE).actif(true).service(transport).build();
    final DossierEmploye dossier = DossierEmploye.builder().id(5L).utilisateur(employe).build();

    private static Utilisateur compte(long id, Role role) { return Utilisateur.builder().id(id).role(role).actif(true).build(); }

    @Test void rhToutEtModification() {
        assertEquals(Habilitation.COMPLETE, HabilitationService.pour(compte(1, Role.RH), dossier));
    }

    @Test void dgLitToutSansModifier() {
        var h = HabilitationService.pour(compte(3, Role.DG), dossier);
        assertTrue(h.salaire() && h.documents() && h.audit());
        assertFalse(h.modification());
    }

    @Test void employeVoitSonDossierMaisPasLeJournalNiCeluiDesAutres() {
        var h = HabilitationService.pour(employe, dossier);
        assertTrue(h.lecture() && h.salaire() && h.donneesPersonnelles());
        assertFalse(h.audit() || h.modification());
        assertFalse(HabilitationService.pour(compte(99, Role.EMPLOYE), dossier).lecture());
    }

    @Test void chefDuServiceVoitLaSituationProfessionnelleUniquement() {
        var h = HabilitationService.pour(chef, dossier);
        assertTrue(h.lecture());
        assertFalse(h.salaire() || h.donneesPersonnelles() || h.documents() || h.audit());
        assertFalse(HabilitationService.pour(autreChef, dossier).lecture(), "chef d'un autre service");
    }

    @Test void compteDesactiveOuCandidatSansAcces() {
        var inactif = compte(1, Role.RH); inactif.setActif(false);
        assertFalse(HabilitationService.pour(inactif, dossier).lecture());
        assertFalse(HabilitationService.pour(compte(4, Role.CANDIDAT), dossier).lecture());
    }
}
