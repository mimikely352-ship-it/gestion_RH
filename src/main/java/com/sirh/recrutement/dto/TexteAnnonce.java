package com.sirh.recrutement.dto;

import com.sirh.commun.exception.BusinessException;
import java.util.Map;

/** Deux champs de saisie conservés sans perte dans la description existante. */
public final class TexteAnnonce {
    private static final String SEPARATEUR = "\n\nProfil recherché\n";
    private static final String PREFIXE = "Missions principales\n";
    private TexteAnnonce() {}

    public static String composer(String missions, String profil, String descriptionHistorique) {
        if (missions == null && profil == null) return descriptionHistorique;
        if (missions == null || missions.isBlank()) throw new BusinessException("Renseignez les missions principales.");
        if (profil == null || profil.isBlank()) throw new BusinessException("Renseignez le profil recherché.");
        return PREFIXE + missions.trim() + SEPARATEUR + profil.trim();
    }

    public static Map<String, String> separer(String description) {
        String texte = description == null ? "" : description.replace("\r\n", "\n");
        int separation = texte.indexOf(SEPARATEUR);
        // Un ancien texte libre reste intégralement visible, sans découpage supposé.
        if (!texte.startsWith(PREFIXE) || separation < 0) return Map.of("missions", texte, "profil", "");
        return Map.of("missions", texte.substring(PREFIXE.length(), separation),
                "profil", texte.substring(separation + SEPARATEUR.length()));
    }
}
