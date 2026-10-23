package com.sirh.embauche.service;
import com.sirh.embauche.entity.*;
import com.sirh.embauche.repository.*;
import com.sirh.commun.enums.*;
import com.sirh.commun.exception.BusinessException;
import com.sirh.demande.entity.Demande;
import com.sirh.demande.repository.DemandeRepository;
import com.sirh.utilisateur.entity.Utilisateur;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class EffectifServiceTest {
    final DossierEmployeRepository dossiers = mock(DossierEmployeRepository.class);
    final AffectationRepository affectations = mock(AffectationRepository.class);
    final DemandeRepository demandes = mock(DemandeRepository.class);
    final CorrectionDateEntreeRepository corrections = mock(CorrectionDateEntreeRepository.class);
    final EffectifService sut = new EffectifService(dossiers, affectations, demandes, corrections);
    @Test void historiqueFigeEtFormationRealiseeSansChangerLePoste() {
        var utilisateur = Utilisateur.builder().id(1L).email("emp@test.local").build();
        var dossier = DossierEmploye.builder().id(2L).nom("Rabe").prenom("Zary").utilisateur(utilisateur).dateEntree(LocalDate.of(2025,1,1)).build();
        var periode = Affectation.builder().dossier(dossier).actuelle(true).posteLibelle("Chauffeur")
                .serviceLibelle("Transport").origine("EMBAUCHE").build();
        when(dossiers.findByUtilisateurRoleAndUtilisateurActifTrue(eq(Role.EMPLOYE),any())).thenReturn(new PageImpl<>(List.of(dossier)));
        when(affectations.findByDossierIdInOrderByIdAsc(List.of(2L))).thenReturn(List.of(periode));
        when(demandes.findByAuteurIdIn(List.of(1L))).thenReturn(List.of(Demande.builder().auteur(utilisateur)
                .type(TypeDemande.FORMATION).statut(StatutDemande.EXECUTEE).description("Sécurité routière").dateEffet(LocalDate.of(2026,1,1)).build()));
        var e = sut.lister().get(0);
        assertEquals("Chauffeur",e.getPoste()); assertEquals("Transport",e.getService());
        assertEquals(2,e.getHistorique().size()); assertTrue(e.getHistorique().get(1).contains("Sécurité routière"));
        assertEquals("01/01/2025",e.getDateEntree());
    }
    @Test void refuseUneDateEntreeFuture() {
        assertThrows(BusinessException.class, () -> sut.renseignerEntree(1L,LocalDate.now().plusDays(1),"rh@test.local"));
        verifyNoInteractions(corrections);
    }
    @Test void correctionDateEntreeTracee() {
        var dossier = DossierEmploye.builder().id(1L).dateEntree(LocalDate.of(2025,1,1)).build();
        when(dossiers.verrouillerParId(1L)).thenReturn(Optional.of(dossier));
        sut.renseignerEntree(1L,LocalDate.of(2025,1,2),"rh@test.local");
        verify(corrections).save(argThat(c -> "rh@test.local".equals(c.getAuteur()) && c.getAncienneDate().equals(LocalDate.of(2025,1,1))));
        assertEquals(LocalDate.of(2025,1,2),dossier.getDateEntree());
    }
}
