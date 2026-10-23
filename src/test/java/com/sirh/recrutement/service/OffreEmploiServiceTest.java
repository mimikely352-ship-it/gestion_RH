package com.sirh.recrutement.service;

import com.sirh.commun.exception.BusinessException;
import com.sirh.demande.service.DemandeService;
import com.sirh.organisation.repository.PosteRepository;
import com.sirh.organisation.repository.SectionRepository;
import com.sirh.recrutement.repository.OffreEmploiRepository;
import com.sirh.referentiel.repository.DiplomeRepository;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OffreEmploiServiceTest {
    @Test void interditLaPublicationSansEntreprise() {
        var repository = mock(OffreEmploiRepository.class);
        var offre = com.sirh.recrutement.entity.OffreEmploi.builder().id(7L).build();
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(offre));
        var service = new OffreEmploiService(repository, mock(PosteRepository.class), mock(SectionRepository.class),
                mock(DiplomeRepository.class), mock(DemandeService.class));
        assertThrows(BusinessException.class, () -> service.publier(7L));
        assertEquals(com.sirh.commun.enums.StatutOffre.BROUILLON, offre.getStatut());
        verify(repository, never()).save(any());
    }
    @Test void demandeRecrutementRealiseeALaPublicationPasAuBrouillon() {
        var offres=mock(OffreEmploiRepository.class); var postes=mock(PosteRepository.class);
        var demandes=mock(DemandeService.class); var diplomes=mock(DiplomeRepository.class);
        when(postes.findById(1L)).thenReturn(java.util.Optional.of(com.sirh.organisation.entity.Poste.builder().intitule("Chauffeur").build()));
        when(demandes.obtenir(12L)).thenReturn(com.sirh.demande.entity.Demande.builder().id(12L)
                .type(com.sirh.commun.enums.TypeDemande.RECRUTEMENT).statut(com.sirh.commun.enums.StatutDemande.EN_EXECUTION).build());
        when(offres.save(any())).thenAnswer(i->{
            com.sirh.recrutement.entity.OffreEmploi o=i.getArgument(0); o.setId(99L);
            when(offres.findById(99L)).thenReturn(java.util.Optional.of(o)); return o;
        });
        var sut=new OffreEmploiService(offres,postes,mock(SectionRepository.class),diplomes,demandes);
        var brouillon = sut.creer(1L,"Chauffeur","Missions",1,null,60,null,Set.of(),12L);
        brouillon.setEntreprise(com.sirh.organisation.entity.Entreprise.builder().id(1L).nom("Entreprise A").build());
        verify(demandes,never()).marquerExecutee(12L);
        sut.publier(99L);
        verify(demandes).marquerExecutee(12L);
    }
    @Test void brouillonNonAccessiblePubliquement() {
        var repo=mock(OffreEmploiRepository.class);
        when(repo.findById(1L)).thenReturn(java.util.Optional.of(com.sirh.recrutement.entity.OffreEmploi.builder()
                .statut(com.sirh.commun.enums.StatutOffre.BROUILLON).build()));
        var sut=new OffreEmploiService(repo,mock(PosteRepository.class),mock(SectionRepository.class),mock(DiplomeRepository.class),mock(DemandeService.class));
        assertThrows(com.sirh.commun.exception.ResourceNotFoundException.class,()->sut.obtenirPublique(1L));
    }
    @Test void normaliseLesPiecesLibresEtIgnoreLesDoublons() {
        var service = new OffreEmploiService(mock(OffreEmploiRepository.class), mock(PosteRepository.class),
                mock(SectionRepository.class), mock(DiplomeRepository.class), mock(DemandeService.class));
        assertEquals(Set.of("Attestation", "Portfolio"), service.lirePiecesRequises(" Attestation \n\nPortfolio\nattestation "));
        assertTrue(service.lirePiecesRequises(null).isEmpty());
        assertThrows(BusinessException.class, () -> service.lirePiecesRequises("x".repeat(101)));
        assertThrows(BusinessException.class, () -> service.lirePiecesRequises("1\n2\n3\n4\n5\n6\n7\n8\n9"));
    }

    @Test void modifieLeBrouillonSansCreerDePosteNiChangerSonStatut() {
        var repository = mock(OffreEmploiRepository.class);
        var postes = mock(PosteRepository.class);
        var diplomes = mock(DiplomeRepository.class);
        var poste = com.sirh.organisation.entity.Poste.builder().id(1L).intitule("Designer").build();
        var offre = com.sirh.recrutement.entity.OffreEmploi.builder().id(7L).poste(poste).titre("Ancien titre").build();
        offre.getDiplomesRequis().add(new com.sirh.referentiel.entity.Diplome());
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(offre));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(diplomes.findAllById(Set.of())).thenReturn(java.util.List.of());
        var service = new OffreEmploiService(repository, postes, mock(SectionRepository.class), diplomes, mock(DemandeService.class));
        var resultat = service.modifierBrouillon(7L, "Designer senior", "Nouvelles missions", 3, 24, null, Set.of());
        assertSame(offre, resultat);
        assertSame(poste, resultat.getPoste());
        assertEquals("Designer senior", resultat.getTitre());
        assertEquals(3, resultat.getNbPostes());
        assertEquals(24, resultat.getExperienceMinMois());
        assertTrue(resultat.getDiplomesRequis().isEmpty());
        assertEquals(com.sirh.commun.enums.StatutOffre.BROUILLON, resultat.getStatut());
        verifyNoInteractions(postes);
    }

    @Test void interditLaModificationDuneOffrePubliee() {
        var repository = mock(OffreEmploiRepository.class);
        var offre = com.sirh.recrutement.entity.OffreEmploi.builder().id(7L).titre("Titre initial")
                .statut(com.sirh.commun.enums.StatutOffre.PUBLIEE).build();
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(offre));
        var service = new OffreEmploiService(repository, mock(PosteRepository.class), mock(SectionRepository.class),
                mock(DiplomeRepository.class), mock(DemandeService.class));
        assertThrows(BusinessException.class, () -> service.modifierBrouillon(7L, "Autre titre", "Missions", 1, null, null, Set.of()));
        assertEquals("Titre initial", offre.getTitre());
        verify(repository, never()).save(any());
    }

    @Test void refuseLaPublicationDunBrouillonExpire() {
        var repository = mock(OffreEmploiRepository.class);
        var offre = com.sirh.recrutement.entity.OffreEmploi.builder().id(7L)
                .dateLimite(java.time.LocalDate.now().minusDays(1)).build();
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(offre));
        var service = new OffreEmploiService(repository, mock(PosteRepository.class), mock(SectionRepository.class),
                mock(DiplomeRepository.class), mock(DemandeService.class));
        assertThrows(BusinessException.class, () -> service.publier(7L));
        assertEquals(com.sirh.commun.enums.StatutOffre.BROUILLON, offre.getStatut());
        verify(repository, never()).save(any());
    }

    @Test void respecteLaLimiteDuTitreEnBaseAvantTouteEcriture() {
        var repository = mock(OffreEmploiRepository.class);
        var postes = mock(PosteRepository.class);
        when(postes.findById(1L)).thenReturn(java.util.Optional.of(
                com.sirh.organisation.entity.Poste.builder().intitule("Designer").build()));
        var service = new OffreEmploiService(repository, postes, mock(SectionRepository.class),
                mock(DiplomeRepository.class), mock(DemandeService.class));
        assertThrows(BusinessException.class, () -> service.creer(1L, "x".repeat(151), "Missions", 1, null, 60, null, Set.of()));
        verify(repository, never()).save(any());
    }

    @Test void creeUneOffrePersonnaliseeSansCategorieNiSection() {
        var offres = mock(OffreEmploiRepository.class);
        var postes = mock(PosteRepository.class);
        var sections = mock(SectionRepository.class);
        var diplomes = mock(DiplomeRepository.class);
        var service = new OffreEmploiService(offres, postes, sections, diplomes, mock(DemandeService.class));
        when(postes.save(any())).thenAnswer(i -> {
            com.sirh.organisation.entity.Poste poste = i.getArgument(0);
            poste.setId(99L);
            when(postes.findById(99L)).thenReturn(java.util.Optional.of(poste));
            return poste;
        });
        when(diplomes.findAllById(Set.of())).thenReturn(java.util.List.of());
        when(offres.save(any())).thenAnswer(i -> i.getArgument(0));
        var offre = service.creerAvecPoste(null, "  Designer graphique  ", "", null,
                "", "Création de visuels pour nos clients", 2, null, 60, null, Set.of(), null);
        assertEquals("Designer graphique", offre.getTitre());
        assertEquals("Designer graphique", offre.getPoste().getIntitule());
        assertNull(offre.getPoste().getSection());
        assertNull(offre.getPoste().getCategorie());
        assertEquals(com.sirh.commun.enums.StatutOffre.BROUILLON, offre.getStatut());
        verifyNoInteractions(sections);
    }

    @Test void reutiliseUnPosteExistantSansEnCreerUnAutre() {
        var offres = mock(OffreEmploiRepository.class);
        var postes = mock(PosteRepository.class);
        var sections = mock(SectionRepository.class);
        var diplomes = mock(DiplomeRepository.class);
        var poste = com.sirh.organisation.entity.Poste.builder().id(1L).intitule("Comptable").build();
        when(postes.findById(1L)).thenReturn(java.util.Optional.of(poste));
        when(diplomes.findAllById(Set.of())).thenReturn(java.util.List.of());
        when(offres.save(any())).thenAnswer(i -> i.getArgument(0));
        var service = new OffreEmploiService(offres, postes, sections, diplomes, mock(DemandeService.class));
        var offre = service.creerAvecPoste(1L, null, null, null,
                "Comptable confirmé", "Tenue des comptes", 1, 12, 60, null, Set.of(), null);
        assertSame(poste, offre.getPoste());
        assertEquals("Comptable confirmé", offre.getTitre());
        verify(postes, never()).save(any());
    }

    @Test void neCreePasDePosteSiLaSectionChoisieEstInconnue() {
        var postes = mock(PosteRepository.class);
        var sections = mock(SectionRepository.class);
        when(sections.findById(999L)).thenReturn(java.util.Optional.empty());
        var service = new OffreEmploiService(mock(OffreEmploiRepository.class), postes, sections,
                mock(DiplomeRepository.class), mock(DemandeService.class));
        assertThrows(BusinessException.class, () -> service.creerAvecPoste(null, "Commercial",
                "Vente", 999L, "", "Missions", 1, null, 60, null, Set.of(), null));
        verify(postes, never()).save(any());
    }

    @Test void refuseExperienceNegativeAvantTouteEcriture() {
        var repository = mock(OffreEmploiRepository.class);
        var postes = mock(PosteRepository.class);
        var service = new OffreEmploiService(repository, postes, mock(SectionRepository.class),
                mock(DiplomeRepository.class), mock(DemandeService.class));
        assertThrows(BusinessException.class,
                () -> service.creer(1L, "Java", "Description", 1, -1, 60, null, Set.of()));
        verifyNoInteractions(repository, postes);
    }
}
