package com.jclinical.attachments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regresion: el tipo de archivo se validaba con el Content-Type de la peticion, que lo
 * elige quien sube el archivo. Bastaba con declarar "image/png" para almacenar cualquier
 * cosa —un HTML o un ejecutable— dentro del expediente.
 */
class AttachmentServiceTest {

    private static final byte[] PNG_HEADER =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private AttachmentService service;
    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        SpringDataAttachmentRepository repository = mock(SpringDataAttachmentRepository.class);
        when(repository.save(any(AttachmentEntity.class))).thenAnswer(call -> call.getArgument(0));
        service = new AttachmentService(repository);
        ReflectionTestUtils.setField(service, "uploadDirectory", tempDir.toString());
    }

    private MockMultipartFile file(String declaredContentType, byte[] content) {
        return new MockMultipartFile("file", "estudio.png", declaredContentType, content);
    }

    @Test
    void rejectsHtmlDisguisedAsPng() {
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.store(clinicId, patientId, "elemento", file("image/png", html)));

        assertEquals("Tipo de archivo no permitido. Solo se aceptan PDF, JPG y PNG.", error.getMessage());
    }

    @Test
    void acceptsARealPngEvenWithAWrongDeclaredContentType() throws IOException {
        // El contenido manda: el Content-Type declarado es irrelevante en ambos sentidos.
        AttachmentEntity saved = service.store(
                clinicId, patientId, "elemento", file("application/octet-stream", PNG_HEADER));

        assertEquals("image/png", saved.getContentType());
    }

    @Test
    void storesTheDetectedTypeNotTheDeclaredOne() {
        byte[] pdf = "%PDF-1.7\n...".getBytes(StandardCharsets.US_ASCII);

        AttachmentEntity saved = service.store(clinicId, patientId, "elemento", file("image/jpeg", pdf));

        assertEquals("application/pdf", saved.getContentType());
    }

    @Test
    void rejectsEmptyFiles() {
        assertThrows(IllegalArgumentException.class,
                () -> service.store(clinicId, patientId, "elemento", file("image/png", new byte[0])));
    }

    @Test
    void rejectsContentTooShortToIdentify() {
        assertThrows(IllegalArgumentException.class,
                () -> service.store(clinicId, patientId, "elemento", file("image/png", new byte[]{0x01, 0x02})));
    }
}
