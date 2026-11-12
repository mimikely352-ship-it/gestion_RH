package com.sirh.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;

import static org.mockito.Mockito.*;

class SchemaMigrationConfigTest {
    @Test void sansActivationExpliciteAucuneConnexionNiMigrationNestExecutee() throws Exception {
        var base = mock(DataSource.class);
        new SchemaMigrationConfig().schemaMigration(base,new MockEnvironment()).afterPropertiesSet();
        verifyNoInteractions(base);
    }
}
