package com.jclinical.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication(scanBasePackages = "com.jclinical")
@EnableJpaRepositories(basePackages = "com.jclinical")
@EntityScan(basePackages = "com.jclinical")
@EnableScheduling
@Slf4j
public class ClineryaApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClineryaApplication.class, args);
    }

    /**
     * Interruptor manual de un solo uso para corregir un flyway_schema_history desalineado
     * (p. ej. una base de datos local restaurada desde otro ambiente con migraciones que no
     * existen en este checkout). Se activa solo con app.flyway.repair-once=true
     * (env var APP_FLYWAY_REPAIR_ONCE=true) — nunca corre por defecto. Enciéndela, arranca una
     * vez, y vuelve a apagarla: dejarla prendida permanentemente oculta futuros problemas reales
     * de migración en lugar de fallar de forma visible.
     */
    @Bean
    @ConditionalOnProperty(name = "app.flyway.repair-once", havingValue = "true")
    public FlywayMigrationStrategy flywayRepairOnceStrategy() {
        return flyway -> {
            log.warn(">>>> app.flyway.repair-once=true: ejecutando flyway.repair() antes de migrate(). "
                    + "Quita esta variable de entorno despues de este arranque.");
            flyway.repair();
            flyway.migrate();
        };
    }
}