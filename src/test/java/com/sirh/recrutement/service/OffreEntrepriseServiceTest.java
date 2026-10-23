package com.sirh.recrutement.service;

import com.sirh.commun.exception.BusinessException;
import com.sirh.organisation.entity.Entreprise;
import com.sirh.organisation.repository.EntrepriseRepository;
import com.sirh.recrutement.entity.OffreEmploi;
import com.sirh.recrutement.repository.OffreEmploiRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OffreEntrepriseServiceTest {
    @Test void entrepriseObligatoireAvantCreationDuPosteEtDeLoffre() {
        var entreprises = mock(EntrepriseRepository.class);
        var offres = mock(OffreEmploiRepository.class);
        var metier = mock(OffreEmploiService.class);
        var service = new OffreEntrepriseService(entreprises, offres, metier);
        assertThrows(BusinessException.class, () -> service.creer(null, false, null, "Poste",
                null, null, "Poste", "Missions", 1, null, 60, null, Set.of(), null, null));
        assertThrows(BusinessException.class, () -> service.creer(99L, true, null, "Poste",
                null, null, "Poste", "Missions", 1, null, 60, null, Set.of(), null, null));
        verifyNoInteractions(offres, metier);
    }

    @Test void attribueLEntrepriseAvantLaPublication() {
        var entreprises = mock(EntrepriseRepository.class);
        var offres = mock(OffreEmploiRepository.class);
        var metier = mock(OffreEmploiService.class);
        var entreprise = Entreprise.builder().id(4L).nom("Entreprise A").build();
        var offre = OffreEmploi.builder().id(8L).build();
        when(entreprises.findById(4L)).thenReturn(Optional.of(entreprise));
        when(metier.creerAvecDossier(any(), any(), any(), any(), any(), any(), anyInt(), any(), anyInt(), any(), any(), any(), any())).thenReturn(offre);
        when(metier.publier(8L)).thenAnswer(i -> {
            assertSame(entreprise, offre.getEntreprise());
            return offre;
        });
        var service = new OffreEntrepriseService(entreprises, offres, metier);
        assertSame(offre, service.creer(4L, true, 1L, null, null, null, "Poste", "Missions",
                1, null, 60, null, Set.of(), null, null));
        var ordre = inOrder(offres, metier);
        ordre.verify(metier).creerAvecDossier(any(), any(), any(), any(), any(), any(), anyInt(), any(), anyInt(), any(), any(), any(), any());
        ordre.verify(offres).save(offre);
        ordre.verify(metier).publier(8L);
    }
}
