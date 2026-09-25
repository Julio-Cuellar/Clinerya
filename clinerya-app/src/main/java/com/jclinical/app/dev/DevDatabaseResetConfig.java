package com.jclinical.app.dev;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * "Drop-create" para la fase de pruebas: en cada arranque vacia la base de datos completa y la
 * vuelve a levantar con las migraciones, asi los seeders (que se saltan si su usuario ya existe)
 * siembran de nuevo las cuentas demo y showcase desde cero.
 *
 * <p>Se usa Flyway y no {@code ddl-auto: create-drop} porque el esquema lo definen las
 * migraciones (esquemas por modulo, catalogos, columnas cifradas); Hibernate generaria otro
 * distinto. Flyway por si solo limpiaria unicamente el esquema por defecto, por eso se le pasan
 * todos los esquemas de la base.
 *
 * <p>Se activa con app.flyway.reset-on-startup=true (env var APP_FLYWAY_RESET_ON_STARTUP) y se
 * niega a arrancar con el perfil prod: borra todos los datos sin preguntar.
 */
@Configuration
@ConditionalOnProperty(name = "app.flyway.reset-on-startup", havingValue = "true")
@Slf4j
public class DevDatabaseResetConfig {

    // @Primary por si alguien enciende tambien app.flyway.repair-once: tras un clean no hay
    // historial que reparar, y dos estrategias sin primaria impiden arrancar.
    @Bean
    @Primary
    public FlywayMigrationStrategy resetDatabaseOnStartup(Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "app.flyway.reset-on-startup borra la base de datos completa y no se permite con el perfil prod.");
        }
        return flyway -> {
            String[] schemas = databaseSchemas(flyway);
            log.warn(">>>> [DEV-RESET] app.flyway.reset-on-startup=true: se borran todos los datos de {} y se "
                    + "recrean con las migraciones.", String.join(", ", schemas));
            Flyway.configure()
                    .configuration(flyway.getConfiguration())
                    .schemas(schemas)
                    .cleanDisabled(false)
                    .load()
                    .clean();
            flyway.migrate();
        };
    }

    // public va primero: ahi vive flyway_schema_history, y clean toma el primer esquema como el suyo.
    private static String[] databaseSchemas(Flyway flyway) {
        String query = "SELECT nspname FROM pg_namespace "
                + "WHERE nspname <> 'information_schema' AND nspname NOT LIKE 'pg\\_%' "
                + "ORDER BY nspname <> 'public', nspname";
        List<String> schemas = new ArrayList<>();
        try (Connection connection = flyway.getConfiguration().getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(query)) {
            while (rows.next()) {
                schemas.add(rows.getString(1));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron listar los esquemas para reiniciar la base de datos.", exception);
        }
        return schemas.toArray(String[]::new);
    }
}
