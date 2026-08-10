package com.jclinical.attachments;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("application/pdf", "image/jpeg", "image/png");
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private final SpringDataAttachmentRepository repository;

    @Value("${app.file-upload.directory:uploads}")
    private String uploadDirectory;

    public AttachmentEntity store(UUID clinicId, UUID patientId, String elementId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo permitido (10 MB).");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Solo se aceptan PDF, JPG y PNG.");
        }

        UUID id = UUID.randomUUID();
        String extension = extensionFor(contentType);
        Path targetDir = Path.of(uploadDirectory, clinicId.toString(), patientId.toString());
        Path targetFile = targetDir.resolve(id + extension);

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetFile);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar el archivo.", ex);
        }

        AttachmentEntity entity = AttachmentEntity.builder()
                .id(id)
                .clinicId(clinicId)
                .patientId(patientId)
                .elementId(elementId)
                .originalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "archivo")
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .storedPath(targetFile.toString())
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(entity);
    }

    public List<AttachmentEntity> listByPatientAndElement(UUID patientId, String elementId) {
        return repository.findByPatientIdAndElementIdOrderByCreatedAtAsc(patientId, elementId);
    }

    public List<AttachmentEntity> listByPatientClinicAndElement(UUID patientId, UUID clinicId, String elementId) {
        return repository.findByPatientIdAndClinicIdAndElementIdOrderByCreatedAtAsc(patientId, clinicId, elementId);
    }

    public AttachmentEntity get(UUID id, UUID patientId) {
        return repository.findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado."));
    }

    public byte[] readContent(AttachmentEntity attachment) {
        try {
            return Files.readAllBytes(Path.of(attachment.getStoredPath()));
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el archivo.", ex);
        }
    }

    public void delete(UUID id, UUID patientId) {
        AttachmentEntity attachment = get(id, patientId);
        try {
            Files.deleteIfExists(Path.of(attachment.getStoredPath()));
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo eliminar el archivo.", ex);
        }
        repository.delete(attachment);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> "";
        };
    }
}
