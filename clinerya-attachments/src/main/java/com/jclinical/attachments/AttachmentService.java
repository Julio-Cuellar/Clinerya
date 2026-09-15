package com.jclinical.attachments;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AttachmentService {

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
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el archivo.", ex);
        }

        // El Content-Type de la peticion lo elige quien sube el archivo, asi que no sirve
        // como control: se determina por los bytes magicos del propio contenido.
        String contentType = detectContentType(content);
        if (contentType == null) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Solo se aceptan PDF, JPG y PNG.");
        }

        UUID id = UUID.randomUUID();
        String extension = extensionFor(contentType);
        Path targetDir = Path.of(uploadDirectory, clinicId.toString(), patientId.toString());
        Path targetFile = targetDir.resolve(id + extension);

        try {
            Files.createDirectories(targetDir);
            Files.write(targetFile, content);
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

    private static final byte[] MAGIC_PDF = {0x25, 0x50, 0x44, 0x46};                        // %PDF
    private static final byte[] MAGIC_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] MAGIC_PNG =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    /**
     * Determina el tipo real por los bytes iniciales. Devuelve null si no es ninguno de
     * los permitidos, incluido el caso de un ejecutable o un HTML renombrado a .png.
     */
    private String detectContentType(byte[] content) {
        if (startsWith(content, MAGIC_PDF)) {
            return "application/pdf";
        }
        if (startsWith(content, MAGIC_JPEG)) {
            return "image/jpeg";
        }
        if (startsWith(content, MAGIC_PNG)) {
            return "image/png";
        }
        return null;
    }

    private boolean startsWith(byte[] content, byte[] magic) {
        return content.length >= magic.length
                && Arrays.equals(Arrays.copyOfRange(content, 0, magic.length), magic);
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
