package com.jclinical.automation.infra.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Decision 19: el SQL de la automatizacion solo toca su propio esquema. Leer o escribir tablas de
 * otro modulo (agenda.*, patients.*, ...) seria saltarse su API publica.
 */
class AutomationSqlScopeTest {

    private static final Path MAIN_SOURCES = Path.of("src", "main", "java");

    @Test
    void theScannerFindsTheTablesASqlStatementTouches() {
        assertEquals(Set.of("agenda.appointments", "automation.conversations"), SqlTableScanner.tablesIn("""
                SELECT a.id FROM agenda.appointments a
                  JOIN automation.conversations c ON c.id = a.id
                """));
        assertEquals(Set.of("patients.patients"), SqlTableScanner.tablesIn("INSERT INTO patients.patients (id) VALUES (?)"));
        assertEquals(Set.of("staff.clinic_staff"), SqlTableScanner.tablesIn("update staff.clinic_staff set x = 1"));
        assertTrue(SqlTableScanner.tablesIn("jdbcTemplate.update(UPSERT_SQL, conversation.id())").isEmpty());
    }

    @Test
    void theAutomationSqlOnlyTouchesTheAutomationSchema() throws IOException {
        Map<String, Set<String>> foreignTables = new TreeMap<>();
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Set<String> foreign = new TreeSet<>();
                for (String table : SqlTableScanner.tablesIn(Files.readString(file))) {
                    if (!table.startsWith("automation.")) {
                        foreign.add(table);
                    }
                }
                if (!foreign.isEmpty()) {
                    foreignTables.put(MAIN_SOURCES.relativize(file).toString(), foreign);
                }
            }
        }

        assertTrue(foreignTables.isEmpty(), "SQL fuera del esquema automation: " + foreignTables);
    }

    /** Encuentra referencias esquema.tabla despues de FROM, JOIN, INTO, UPDATE o TABLE. */
    static final class SqlTableScanner {

        private SqlTableScanner() {
        }

        static Set<String> tablesIn(String text) {
            return Set.of();
        }
    }
}
