package com.sirh.embauche.service;
import com.sirh.embauche.entity.*;
import com.sirh.embauche.repository.*;
import com.sirh.commun.enums.TypeDemande;
import com.sirh.commun.exception.BusinessException;
import com.sirh.demande.entity.Demande;
import com.sirh.organisation.entity.*;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import com.sirh.organisation.repository.PosteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDate;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ParcoursEmployeServiceTest {
    final DossierEmployeRepository dossiers = mock(DossierEmployeRepository.class);
    final AffectationRepository affectations = mock(AffectationRepository.class);
    final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    final PosteRepository postes = mock(PosteRepository.class);
    final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    final ParcoursEmployeService sut = new ParcoursEmployeService(dossiers, affectations, utilisateurs, postes, passwordEncoder);
    final Utilisateur employe = Utilisateur.builder().id(1L).build();
    final DossierEmploye dossier = DossierEmploye.builder().id(2L).dateEntree(LocalDate.of(2020,1,1)).build();
    final Affectation actuelle = Affectation.builder().id(3L).actuelle(true).dateDebut(LocalDate.of(2020,1,1))
            .poste(Poste.builder().id(4L).intitule("Libellé renommé").build()).posteLibelle("Chauffeur").build();
    Demande demande() {
        when(dossiers.verrouillerParUtilisateur(1L)).thenReturn(Optional.of(dossier));
        when(affectations.findByDossierIdAndActuelleTrue(2L)).thenReturn(Optional.of(actuelle));
        return Demande.builder().id(5L).auteur(employe).type(TypeDemande.TRANSFERT).affectationOrigineId(3L)
                .posteCible(Poste.builder().id(6L).intitule("Comptable").section(Section.builder()
                        .service(Service.builder().id(7L).nom("Finance").build()).build()).build()).build();
    }
    @Test void clotureAncienneAffectationEtConserveEntreeEtLibellesHistoriques() {
        sut.appliquerMouvement(demande(), LocalDate.now());
        assertFalse(actuelle.isActuelle()); assertEquals(LocalDate.now(),actuelle.getDateFin());
        assertEquals("Chauffeur",actuelle.getPosteLibelle()); assertEquals(LocalDate.of(2020,1,1),dossier.getDateEntree());
        verify(affectations).save(argThat(a -> a.isActuelle() && "Comptable".equals(a.getPosteLibelle()) && "Finance".equals(a.getServiceLibelle())));
        assertEquals(7L,employe.getService().getId());
    }
    @Test void refuseUnMouvementFondeSurUneAncienneAffectation() {
        var d=demande(); d.setAffectationOrigineId(999L);
        assertThrows(BusinessException.class,()->sut.appliquerMouvement(d,LocalDate.now()));
        verify(affectations,never()).saveAndFlush(any());
    }
    @Test void refuseDateFutureSansAppliquerLePoste() {
        var d=demande(); assertThrows(BusinessException.class,()->sut.appliquerMouvement(d,LocalDate.now().plusDays(1)));
        assertTrue(actuelle.isActuelle()); verify(affectations,never()).save(any());
    }

    Poste posteAvecService(Long id, String intitule, Long serviceId, String serviceNom) {
        return Poste.builder().id(id).intitule(intitule)
                .section(Section.builder().service(Service.builder().id(serviceId).nom(serviceNom).build()).build()).build();
    }

    @Test void embaucheDirectementCreeUtilisateurDossierEtAffectation() {
        var poste = posteAvecService(8L, "Chauffeur", 9L, "Transport");
        when(utilisateurs.existsByEmail("nouveau@sirh.local")).thenReturn(false);
        when(postes.findById(8L)).thenReturn(Optional.of(poste));
        when(passwordEncoder.encode("motdepasse123")).thenReturn("hash");
        when(utilisateurs.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dossiers.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var utilisateur = sut.embaucherDirectement("nouveau@sirh.local", "motdepasse123", "Dupont", "Jean", 8L, LocalDate.of(2024,1,1));

        assertEquals("nouveau@sirh.local", utilisateur.getEmail());
        assertEquals("hash", utilisateur.getMotDePasse());
        assertEquals(9L, utilisateur.getService().getId());
        verify(affectations).save(argThat(a -> a.isActuelle() && "EMBAUCHE_DIRECTE".equals(a.getOrigine()) && "Chauffeur".equals(a.getPosteLibelle())));
    }

    @Test void embaucheDirectementRefuseEmailDejaUtilise() {
        when(utilisateurs.existsByEmail("existe@sirh.local")).thenReturn(true);
        assertThrows(BusinessException.class, () ->
                sut.embaucherDirectement("existe@sirh.local", "motdepasse123", "Dupont", "Jean", 8L, null));
        verify(affectations, never()).save(any());
    }

    @Test void promouvoirDirectementClotureAncienneEtOuvreNouvelleAffectationSansDemande() {
        var utilisateurCible = Utilisateur.builder().id(20L).build();
        var dossierCible = DossierEmploye.builder().id(2L).utilisateur(utilisateurCible).dateEntree(LocalDate.of(2020,1,1)).build();
        var affectationActuelle = Affectation.builder().id(3L).actuelle(true).dateDebut(LocalDate.of(2020,1,1))
                .poste(Poste.builder().id(4L).intitule("Chauffeur").build()).posteLibelle("Chauffeur").build();
        var nouveauPoste = posteAvecService(11L, "Chef d’équipe", 9L, "Transport");
        when(dossiers.verrouillerParId(2L)).thenReturn(Optional.of(dossierCible));
        when(affectations.findByDossierIdAndActuelleTrue(2L)).thenReturn(Optional.of(affectationActuelle));
        when(postes.findById(11L)).thenReturn(Optional.of(nouveauPoste));

        sut.promouvoirDirectement(2L, 11L, LocalDate.now());

        assertFalse(affectationActuelle.isActuelle());
        verify(affectations).save(argThat(a -> a.isActuelle() && "PROMOTION_RH".equals(a.getOrigine()) && "Chef d’équipe".equals(a.getPosteLibelle())));
        assertEquals(9L, utilisateurCible.getService().getId());
    }
}
