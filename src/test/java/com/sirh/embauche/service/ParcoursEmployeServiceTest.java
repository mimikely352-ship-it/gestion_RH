package com.sirh.embauche.service;
import com.sirh.embauche.entity.*;
import com.sirh.embauche.repository.*;
import com.sirh.commun.enums.TypeDemande;
import com.sirh.commun.exception.BusinessException;
import com.sirh.demande.entity.Demande;
import com.sirh.organisation.entity.*;
import com.sirh.utilisateur.entity.Utilisateur;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ParcoursEmployeServiceTest {
    final DossierEmployeRepository dossiers = mock(DossierEmployeRepository.class);
    final AffectationRepository affectations = mock(AffectationRepository.class);
    final ParcoursEmployeService sut = new ParcoursEmployeService(dossiers, affectations);
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
}
