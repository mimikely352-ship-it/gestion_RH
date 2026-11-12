package com.sirh.presence.service;
import com.sirh.commun.enums.Role;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.embauche.repository.*;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AccesPresenceServiceTest {
    private final UtilisateurRepository utilisateurs=mock(UtilisateurRepository.class);
    private final DossierEmployeRepository dossiers=mock(DossierEmployeRepository.class);
    private final AccesPresenceService service=new AccesPresenceService(utilisateurs,dossiers,mock(AuditDossierRepository.class),java.time.Clock.systemUTC());
    @Test void proprieteEtRoleRhVerifiesDepuisLaBase() {
        var auth=new UsernamePasswordAuthenticationToken("emp","");
        var u=Utilisateur.builder().id(1L).actif(true).role(Role.EMPLOYE).build();
        var d=DossierEmploye.builder().id(2L).utilisateur(Utilisateur.builder().id(2L).build()).build();
        when(utilisateurs.findByEmail("emp")).thenReturn(Optional.of(u));when(dossiers.findById(2L)).thenReturn(Optional.of(d));
        assertThrows(AccessDeniedException.class,() -> service.lire(2L,auth));
        assertThrows(AccessDeniedException.class,() -> service.exigerRh(auth));
        u.setRole(Role.RH);assertSame(d,service.lire(2L,auth));
        u.setActif(false);assertThrows(AccessDeniedException.class,() -> service.lire(2L,auth));
    }
    @Test void candidatSansAccesMemeAvecDossier() {
        var auth=new UsernamePasswordAuthenticationToken("candidat","");
        when(utilisateurs.findByEmail("candidat")).thenReturn(Optional.of(Utilisateur.builder().actif(true).role(Role.CANDIDAT).build()));
        assertThrows(AccessDeniedException.class,() -> service.personnel(auth));
    }
}
