package com.jclinical.app.web;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

@RestController
@RequestMapping("/api/v1/system-configs")
@RequiredArgsConstructor
@Slf4j
public class SystemConfigController {

    private final SystemConfigRepository repository;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.backup.pg-dump-path:/usr/bin/pg_dump}")
    private String pgDumpPath;

    @GetMapping("/{key}")
    public ResponseEntity<SystemConfigResponse> getConfig(@PathVariable String key) {
        return repository.findById(key)
                .map(entity -> ResponseEntity.ok(new SystemConfigResponse(entity.getKey(), entity.getValue(), entity.getDescription())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{key}")
    public ResponseEntity<SystemConfigResponse> updateConfig(@PathVariable String key, @RequestBody UpdateConfigRequest request) {
        SystemConfigEntity entity = repository.findById(key)
                .orElseGet(() -> {
                    SystemConfigEntity newEntity = new SystemConfigEntity();
                    newEntity.setKey(key);
                    return newEntity;
                });
        entity.setValue(request.value());
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        SystemConfigEntity saved = repository.save(entity);
        return ResponseEntity.ok(new SystemConfigResponse(saved.getKey(), saved.getValue(), saved.getDescription()));
    }

    @PostMapping("/backups/trigger")
    @SuppressWarnings("java:S4036")
    public void triggerBackup(HttpServletResponse response) {
        try {
            String cleanUrl = dbUrl.replace("jdbc:postgresql://", "");
            int slashIdx = cleanUrl.indexOf('/');
            if (slashIdx == -1) {
                throw new IllegalArgumentException("URL de base de datos invalida");
            }
            String hostPort = cleanUrl.substring(0, slashIdx);
            String dbName = cleanUrl.substring(slashIdx + 1);

            if (dbName.contains("?")) {
                dbName = dbName.substring(0, dbName.indexOf('?'));
            }

            String host = hostPort;
            String port = "5432";
            if (hostPort.contains(":")) {
                String[] parts = hostPort.split(":");
                host = parts[0];
                port = parts[1];
            }

            Path pgDumpExecutable = Paths.get(pgDumpPath).normalize();
            if (!pgDumpExecutable.isAbsolute()) {
                throw new IllegalStateException("La ruta de pg_dump debe ser absoluta y no resolverse desde PATH");
            }

            ProcessBuilder pb = new ProcessBuilder(
                    pgDumpExecutable.toString(),
                    "-h", host,
                    "-p", port,
                    "-U", dbUsername,
                    "-d", dbName
            );
            pb.environment().put("PGPASSWORD", dbPassword);

            String filename = "backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".sql.gz";
            response.setContentType("application/gzip");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            log.info("Iniciando pg_dump manual para base de datos {} en {}:{} usando {}", dbName, host, port, pgDumpExecutable);
            Process process = pb.start();

            try (InputStream pgIn = process.getInputStream();
                 GZIPOutputStream gzipOut = new GZIPOutputStream(response.getOutputStream())) {
                pgIn.transferTo(gzipOut);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("pg_dump manual fallo con codigo de salida {}", exitCode);
            } else {
                log.info("pg_dump manual completado exitosamente y transmitido al cliente.");
            }
        } catch (java.io.IOException ioe) {
            log.error("No se pudo ejecutar pg_dump desde la ruta configurada {}.", pgDumpPath, ioe);
            writeError(response, HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo ejecutar pg_dump desde la ruta configurada. Valida app.backup.pg-dump-path y que el archivo exista en el servidor.");
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            log.error("La generacion del respaldo fue interrumpida.", interruptedException);
            writeError(response, HttpStatus.INTERNAL_SERVER_ERROR, "La generacion del respaldo fue interrumpida.");
        } catch (Exception e) {
            log.error("Error al generar respaldo manual", e);
            writeError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el respaldo.");
        }
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) {
        try {
            if (!response.isCommitted()) {
                response.reset();
                response.setStatus(status.value());
                response.setContentType("application/json;charset=UTF-8");
                String json = "{\"message\":\"" + message + "\"}";
                response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
    }

    public record SystemConfigResponse(String key, String value, String description) {
    }

    public record UpdateConfigRequest(String value, String description) {
    }
}
