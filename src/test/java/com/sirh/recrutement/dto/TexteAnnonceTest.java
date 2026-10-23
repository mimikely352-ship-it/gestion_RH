package com.sirh.recrutement.dto;

import com.sirh.commun.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TexteAnnonceTest {
    @Test void conserveLesDeuxPartiesDeLAnnonce() {
        String texte = TexteAnnonce.composer("Accueil\nSuivi", "Organisation\nDiscrétion", null);
        assertEquals("Accueil\nSuivi", TexteAnnonce.separer(texte).get("missions"));
        assertEquals("Organisation\nDiscrétion", TexteAnnonce.separer(texte).get("profil"));
    }
    @Test void conserveIntegralemementLesAnciennesDescriptions() {
        String ancien = "Missions diverses\nProfil et avantages renseignés librement";
        assertEquals(ancien, TexteAnnonce.composer(null, null, ancien));
        assertEquals(ancien, TexteAnnonce.separer(ancien).get("missions"));
        assertEquals("", TexteAnnonce.separer(ancien).get("profil"));
    }
    @Test void refuseUnePartieVideDansLeNouveauFormulaire() {
        assertThrows(BusinessException.class, () -> TexteAnnonce.composer("   ", "Profil", null));
        assertThrows(BusinessException.class, () -> TexteAnnonce.composer("Missions", "   ", null));
    }
}
