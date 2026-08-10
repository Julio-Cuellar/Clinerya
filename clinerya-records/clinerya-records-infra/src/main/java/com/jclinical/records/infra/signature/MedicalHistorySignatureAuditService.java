package com.jclinical.records.infra.signature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jclinical.records.domain.model.DocumentSignature;
import com.jclinical.records.domain.model.DocumentSignatureStatus;
import com.jclinical.records.domain.model.MedicalHistory;
import com.jclinical.records.domain.model.SignatureDocumentType;
import com.jclinical.records.domain.model.SignerType;
import com.jclinical.records.domain.ports.out.DocumentSignatureRepositoryPort;
import com.jclinical.records.domain.ports.out.MedicalHistoryTemplateRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicalHistorySignatureAuditService {

    private final ObjectMapper objectMapper;
    private final MedicalHistoryTemplateRepositoryPort templateRepository;
    private final DocumentSignatureRepositoryPort signatureRepository;

    @Transactional
    public void recordSignatures(
            MedicalHistory history,
            String previousAnswersJson,
            UUID currentUserId,
            String currentUserName,
            String ipAddress,
            String userAgent) {
        var templateOpt = templateRepository.findByIdAndClinicId(history.getTemplateId(), history.getClinicId());
        if (templateOpt.isEmpty()) {
            return;
        }

        JsonNode answers = parseJson(history.getAnswersJson());
        JsonNode previousAnswers = parseJson(previousAnswersJson);
        JsonNode schema = parseJson(templateOpt.get().getSchemaJson());
        if (!answers.isObject() || !schema.isObject()) {
            return;
        }

        List<SignatureField> fields = signatureFields(schema);
        if (fields.isEmpty()) {
            return;
        }

        String documentHash = calculateMedicalHistoryHash(history);
        LocalDateTime signedAt = LocalDateTime.now();
        boolean documentContentChanged = contentWithoutSignaturesChanged(answers, previousAnswers, fields);

        for (SignatureField field : fields) {
            String signatureValue = textValue(answers, field.id());
            if (!isCapturedSignature(signatureValue)) {
                continue;
            }
            String previousValue = textValue(previousAnswers, field.id());
            if (signatureValue.equals(previousValue)) {
                if (documentContentChanged) {
                    signatureRepository.supersedeActiveSignatures(SignatureDocumentType.MEDICAL_HISTORY, history.getId(), field.id());
                }
                continue;
            }

            SignerType signerType = "signature_doctor".equals(field.type()) ? SignerType.DOCTOR : SignerType.PATIENT;
            signatureRepository.supersedeActiveSignatures(SignatureDocumentType.MEDICAL_HISTORY, history.getId(), field.id());
            signatureRepository.save(DocumentSignature.builder()
                    .id(UUID.randomUUID())
                    .documentType(SignatureDocumentType.MEDICAL_HISTORY)
                    .documentId(history.getId())
                    .clinicId(history.getClinicId())
                    .patientId(history.getPatientId())
                    .signerType(signerType)
                    .signerUserId(signerType == SignerType.DOCTOR ? currentUserId : null)
                    .signerName(resolveSignerName(signerType, answers, currentUserName))
                    .signatureFieldId(field.id())
                    .signatureFieldLabel(field.label())
                    .signatureImageHash(sha256(signatureValue))
                    .documentHash(documentHash)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status(DocumentSignatureStatus.ACTIVE)
                    .signedAt(signedAt)
                    .createdAt(signedAt)
                    .build());
        }
    }

    private JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private List<SignatureField> signatureFields(JsonNode schema) {
        List<SignatureField> fields = new ArrayList<>();
        JsonNode pages = schema.path("pages");
        if (pages.isArray()) {
            pages.forEach(page -> collectSignatureFields(page.path("elements"), fields));
        }
        collectSignatureFields(schema.path("elements"), fields);
        return fields;
    }

    private void collectSignatureFields(JsonNode elements, List<SignatureField> fields) {
        if (!elements.isArray()) {
            return;
        }
        elements.forEach(element -> {
            String type = element.path("type").asText("");
            if ("signature_patient".equals(type) || "signature_doctor".equals(type)) {
                fields.add(new SignatureField(
                        element.path("id").asText(""),
                        element.path("label").asText(type),
                        type
                ));
            }
        });
    }

    private boolean contentWithoutSignaturesChanged(JsonNode answers, JsonNode previousAnswers, List<SignatureField> fields) {
        if (!previousAnswers.isObject()) {
            return false;
        }
        ObjectNode currentComparable = answers.deepCopy();
        ObjectNode previousComparable = previousAnswers.deepCopy();
        for (SignatureField field : fields) {
            currentComparable.remove(field.id());
            previousComparable.remove(field.id());
        }
        return !currentComparable.equals(previousComparable);
    }

    private String textValue(JsonNode node, String key) {
        if (node == null || key == null || key.isBlank()) {
            return "";
        }
        JsonNode value = node.path(key);
        return value.isTextual() ? value.asText() : "";
    }

    private boolean isCapturedSignature(String value) {
        return value != null && value.startsWith("data:image/") && value.contains("base64,");
    }

    private String resolveSignerName(SignerType signerType, JsonNode answers, String currentUserName) {
        if (signerType == SignerType.DOCTOR) {
            return isBlank(currentUserName) ? "Medico" : currentUserName.trim();
        }
        String[] patientNameKeys = { "nombre", "patientName", "nombrePaciente", "fullName" };
        for (String key : patientNameKeys) {
            String value = textValue(answers, key);
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "Paciente";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String calculateMedicalHistoryHash(MedicalHistory history) {
        String payload = String.join("\n",
                "MEDICAL_HISTORY",
                value(history.getId()),
                value(history.getPatientId()),
                value(history.getClinicId()),
                value(history.getTemplateId()),
                value(history.getAnswersJson()));
        return sha256(payload);
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo calcular el hash de firma.", e);
        }
    }

    private record SignatureField(String id, String label, String type) {}
}
