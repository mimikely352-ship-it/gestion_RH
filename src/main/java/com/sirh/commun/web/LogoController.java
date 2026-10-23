package com.sirh.commun.web;

import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.ServletContextResource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Sert uniquement le logo choisi, sans exposer le dossier contenant les CV. */
@RestController
public class LogoController {
    private final Path fichier;
    private final ServletContext contexte;

    public LogoController(@Value("${app.brand.logo-file:../uploads/logo.jpg}") String chemin, ServletContext contexte) {
        this.fichier = Path.of(chemin).toAbsolutePath().normalize();
        this.contexte = contexte;
    }

    @GetMapping("/assets/brand/logo")
    public ResponseEntity<Resource> logo() {
        String nom = fichier.getFileName().toString().toLowerCase(Locale.ROOT);
        MediaType type = nom.endsWith(".png") ? MediaType.IMAGE_PNG
                : nom.endsWith(".webp") ? MediaType.parseMediaType("image/webp") : MediaType.IMAGE_JPEG;
        boolean format = nom.endsWith(".jpg") || nom.endsWith(".jpeg") || nom.endsWith(".png") || nom.endsWith(".webp");
        Resource image = format && Files.isRegularFile(fichier) && Files.isReadable(fichier)
                ? new FileSystemResource(fichier) : new ServletContextResource(contexte, "/assets/brand/logo.jpg");
        if (!(image instanceof FileSystemResource)) type = MediaType.IMAGE_JPEG;
        if (!image.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).contentType(type).body(image);
    }
}
