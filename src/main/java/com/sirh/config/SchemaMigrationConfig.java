package com.sirh.config;

import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.nio.charset.StandardCharsets;

/** Migrations recentes, avant validation Hibernate. Le schema initial reste installe par 01_schema.sql. */
@Configuration
public class SchemaMigrationConfig {
    @Bean static BeanFactoryPostProcessor attendreMigrations() {
        return beans -> {
            if (beans.containsBeanDefinition("entityManagerFactory")) {
                var definition = beans.getBeanDefinition("entityManagerFactory");
                var dependances = new java.util.ArrayList<String>();
                if (definition.getDependsOn() != null) dependances.addAll(java.util.List.of(definition.getDependsOn()));
                dependances.add("schemaMigration"); definition.setDependsOn(dependances.toArray(String[]::new));
            }
        };
    }
    @Bean InitializingBean schemaMigration(DataSource dataSource,
            org.springframework.core.env.Environment environnement) {
        return () -> {
            if (!environnement.getProperty("app.schema-migrations.enabled", Boolean.class, true)) return;
            try (var connexion = dataSource.getConnection()) {
                connexion.setAutoCommit(false);
                try {
                    try (var statement = connexion.createStatement()) {
                        statement.execute("SELECT pg_advisory_xact_lock(904103)");
                        statement.execute("CREATE TABLE IF NOT EXISTS app_schema_migration(version VARCHAR(80) PRIMARY KEY, checksum VARCHAR(64) NOT NULL, appliquee_le TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
                    }
                    for (var fichier : java.util.List.of("09_dates_parcours_employes.sql", "10_fiabilisation_metier.sql", "13_fix_candidature_version.sql", "14_rh_admin.sql")) {
                        byte[] octets = new ClassPathResource("db/migration/" + fichier).getContentAsByteArray();
                        String somme = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(octets));
                        try (var requete = connexion.prepareStatement("SELECT checksum FROM app_schema_migration WHERE version=?")) {
                            requete.setString(1, fichier);
                            try (var lignes = requete.executeQuery()) {
                                if (lignes.next()) {
                                    if (!somme.equals(lignes.getString(1))) throw new IllegalStateException("Migration déjà appliquée puis modifiée : " + fichier);
                                    continue;
                                }
                            }
                        }
                        String sql = new String(octets, StandardCharsets.UTF_8)
                                .replaceAll("(?m)^\\\\.*$", "").replaceAll("(?mi)^\\s*(BEGIN|COMMIT);\\s*$", "");
                        ScriptUtils.executeSqlScript(connexion, new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
                        try (var insert = connexion.prepareStatement("INSERT INTO app_schema_migration(version,checksum) VALUES(?,?)")) {
                            insert.setString(1,fichier); insert.setString(2,somme); insert.executeUpdate();
                        }
                    }
                    connexion.commit();
                } catch (Exception erreur) { connexion.rollback(); throw erreur; }
            }
        };
    }
}
