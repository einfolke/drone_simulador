package com.drone.simulador.infra;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

public final class MigrationService {

    private MigrationService() {
    }

    public static void migrate() {
        try {
            Flyway.configure()
                .dataSource(DatabaseConfig.getDataSource())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();
        } catch (FlywayException e) {
            throw new IllegalStateException("Falha ao executar migrações do banco", e);
        }
    }
}
