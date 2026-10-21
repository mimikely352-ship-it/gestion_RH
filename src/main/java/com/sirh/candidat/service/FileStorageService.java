package com.sirh.candidat.service;

import com.sirh.commun.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/** Stockage disque local des pièces jointes (CV). Pas de parsing du contenu, upload simple. */
@Service
public class FileStorageService {

    private final Path racine;
    private final List<String> extensionsAutorisees;

    public FileStorageService(@Value("${app.upload.dir}") String dossier,
                               @Value("${app.upload.cv.extensions-autorisees}") String extensions) {
        this.racine = Paths.get(dossier).toAbsolutePath().normalize();
        this.extensionsAutorisees = List.of(extensions.toLowerCase().split(","));
        try {
            Files.createDirectories(racine);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le dossier d'upload : " + racine, e);
        }
    }

    /** Enregistre le fichier sur disque et renvoie son chemin relatif (stocké dans Document.chemin). */
    public String enregistrer(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Fichier manquant.");
        }
        String extension = extension(fichier.getOriginalFilename());
        if (!extensionsAutorisees.contains(extension)) {
            throw new BusinessException("Format de fichier non autorisé (" + extension + "). Autorisé : "
                    + String.join(", ", extensionsAutorisees));
        }
        String nomStocke = UUID.randomUUID() + "." + extension;
        Path destination = racine.resolve(nomStocke);
        try {
            fichier.transferTo(destination);
        } catch (IOException e) {
            throw new IllegalStateException("Échec de l'enregistrement du fichier", e);
        }
        return nomStocke;
    }

    /** Charge un fichier précédemment enregistré, par son nom stocké (Document.chemin). */
    public Resource charger(String nomStocke) {
        Path fichier = racine.resolve(nomStocke).normalize();
        if (!fichier.getParent().equals(racine) || !Files.exists(fichier)) {
            throw new BusinessException("Fichier introuvable.");
        }
        return new FileSystemResource(fichier);
    }

    private String extension(String nomFichier) {
        String nom = StringUtils.cleanPath(nomFichier == null ? "" : nomFichier);
        int point = nom.lastIndexOf('.');
        if (point < 0 || point == nom.length() - 1) {
            throw new BusinessException("Le fichier doit avoir une extension (." +
                    String.join(", .", extensionsAutorisees) + ").");
        }
        return nom.substring(point + 1).toLowerCase();
    }
}
