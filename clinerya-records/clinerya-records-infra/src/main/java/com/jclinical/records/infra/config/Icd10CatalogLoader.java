package com.jclinical.records.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga el catalogo oficial CIE-10 (CAT_DIAGNOSTICOS de la DGIS, Secretaria de
 * Salud) desde {@code data/icd10_catalog_mx.tsv} en cada arranque. Es un upsert
 * idempotente (INSERT ... ON CONFLICT) para que una actualizacion futura del TSV
 * (nueva version del catalogo publicada por la DGIS) se refleje sin necesidad de
 * una migracion nueva. Fuente: https://www.datos.gob.mx/dataset/catalogo_cie_10
 * (~14,485 codigos vigentes y retirados, no solo el subset clinico comun).
 */
@Slf4j
@Component
public class Icd10CatalogLoader implements ApplicationRunner {

    private static final String RESOURCE_PATH = "data/icd10_catalog_mx.tsv";
    private static final int BATCH_SIZE = 1000;
    private static final String UPSERT_SQL = """
            INSERT INTO records.icd10_catalog (code, description, chapter, billable)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (code) DO UPDATE SET
                description = EXCLUDED.description,
                chapter = EXCLUDED.chapter,
                billable = EXCLUDED.billable
            """;

    private final JdbcTemplate jdbcTemplate;

    public Icd10CatalogLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        int total = 0;
        try (InputStream in = new ClassPathResource(RESOURCE_PATH).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            if (line != null && line.startsWith("code\t")) {
                line = reader.readLine();
            }
            while (line != null) {
                if (!line.isBlank()) {
                    String[] parts = line.split("\t", -1);
                    if (parts.length == 4) {
                        batch.add(new Object[]{parts[0], parts[1], parts[2], Boolean.parseBoolean(parts[3])});
                        total++;
                        if (batch.size() == BATCH_SIZE) {
                            jdbcTemplate.batchUpdate(UPSERT_SQL, batch);
                            batch.clear();
                        }
                    } else {
                        log.warn("Fila invalida en {} ignorada: {}", RESOURCE_PATH, line);
                    }
                }
                line = reader.readLine();
            }
            if (!batch.isEmpty()) {
                jdbcTemplate.batchUpdate(UPSERT_SQL, batch);
            }
        }
        log.info("Catalogo CIE-10 sincronizado: {} codigos procesados desde {}", total, RESOURCE_PATH);
    }
}
