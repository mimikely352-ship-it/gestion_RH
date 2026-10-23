package com.sirh.commun.web;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LogoControllerTest {
    @TempDir Path dossier;

    @Test void sertLeLogoChoisiSansAccepterDeCheminDansLaRequete() throws Exception {
        byte[] contenu = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xd9};
        Path logo = dossier.resolve("logo.jpg"); Files.write(logo, contenu);
        var mvc = MockMvcBuilders.standaloneSetup(new LogoController(logo.toString(), mock(ServletContext.class))).build();
        mvc.perform(get("/assets/brand/logo").param("file", "cv-prive.pdf"))
                .andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Cache-Control", "no-cache")).andExpect(content().bytes(contenu));
        mvc.perform(get("/assets/brand/cv-prive.pdf")).andExpect(status().isNotFound());
    }

    @Test void utiliseLaCopieIncluseDansLeWarSiLeFichierExterneManque() throws Exception {
        byte[] contenu = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xd9};
        Path copie = dossier.resolve("copie.jpg"); Files.write(copie, contenu);
        var contexte = mock(ServletContext.class);
        when(contexte.getResource("/assets/brand/logo.jpg")).thenReturn(copie.toUri().toURL());
        when(contexte.getResourceAsStream("/assets/brand/logo.jpg")).thenAnswer(i -> new java.io.ByteArrayInputStream(contenu));
        MockMvcBuilders.standaloneSetup(new LogoController(dossier.resolve("absent.jpg").toString(), contexte))
                .build().perform(get("/assets/brand/logo"))
                .andExpect(status().isOk()).andExpect(content().bytes(contenu));
    }
}
