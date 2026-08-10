package com.jclinical.app.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    public void triggerBackup(jakarta.servlet.http.HttpServletResponse response) {
        try {
            // Parse jdbc:postgresql://host:port/database
            String cleanUrl = dbUrl.replace("jdbc:postgresql://", "");
            int slashIdx = cleanUrl.indexOf('/');
            if (slashIdx == -1) {
                throw new IllegalArgumentException("URL de base de datos inválida");
            }
            String hostPort = cleanUrl.substring(0, slashIdx);
            String dbName = cleanUrl.substring(slashIdx + 1);
            
            // Si tiene parámetros query al final, los removemos (ej. ?currentSchema=core)
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

            ProcessBuilder pb = new ProcessBuilder(
                    "pg_dump",
                    "-h", host,
                    "-p", port,
                    "-U", dbUsername,
                    "-d", dbName
            );
            pb.environment().put("PGPASSWORD", dbPassword);

            String filename = "backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".sql.gz";
            response.setContentType("application/gzip");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            log.info("Iniciando pg_dump manual para base de datos {} en {}:{}", dbName, host, port);
            Process process = pb.start();

            try (InputStream pgIn = process.getInputStream();
                 GZIPOutputStream gzipOut = new GZIPOutputStream(response.getOutputStream())) {
                pgIn.transferTo(gzipOut);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("pg_dump manual falló con código de salida {}", exitCode);
            } else {
                log.info("pg_dump manual completado exitosamente y transmitido al cliente.");
            }
        } catch (java.io.IOException ioe) {
            log.error("No se pudo ejecutar el comando pg_dump. Valida que esté instalado y en el PATH.", ioe);
            try {
                if (!response.isCommitted()) {
                    response.reset();
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    response.setContentType("application/json;charset=UTF-8");
                    String json = "{\"message\":\"El comando 'pg_dump' no está instalado en el sistema o no se encuentra en el PATH. " +
                                  "En producción con Docker esto funciona de manera nativa. Si estás en desarrollo local, debes instalar las herramientas cliente de PostgreSQL.\"}";
                    response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            log.error("Error al generar respaldo manual", e);
            try {
                if (!response.isCommitted()) {
                    response.reset();
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    response.setContentType("application/json;charset=UTF-8");
                    String json = "{\"message\":\"Error al generar el respaldo: " + e.getMessage() + "\"}";
                    response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {}
        }
    }

    public record SystemConfigResponse(String key, String value, String description) {}
    public record UpdateConfigRequest(String value, String description) {}
}
