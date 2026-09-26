package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.model.RegistrationDraft;
import com.jclinical.automation.domain.ports.out.PatientRegistrationPort.Sex;
import com.jclinical.automation.domain.ports.out.RegistrationDraftPort;
import com.jclinical.core.security.crypto.FieldCipher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Borrador del alta por WhatsApp. Nombre, apellidos y fecha de nacimiento son datos personales: se
 * guardan cifrados. Dura lo que la conversacion; lo abandonado se borra al dia siguiente.
 */
public class JdbcRegistrationDraftRepository implements RegistrationDraftPort {

    private static final String COLUMNS = "conversation_id, clinic_id, consent_version, first_name, last_name_paterno, "
            + "last_name_materno, date_of_birth, sex, updated_at";

    private static final String FIND_SQL =
            "SELECT " + COLUMNS + " FROM automation.registration_drafts WHERE conversation_id = ?";

    private static final String UPSERT_SQL = """
            INSERT INTO automation.registration_drafts (conversation_id, clinic_id, consent_version, first_name,
                last_name_paterno, last_name_materno, date_of_birth, sex, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (conversation_id) DO UPDATE
               SET consent_version = EXCLUDED.consent_version, first_name = EXCLUDED.first_name,
                   last_name_paterno = EXCLUDED.last_name_paterno, last_name_materno = EXCLUDED.last_name_materno,
                   date_of_birth = EXCLUDED.date_of_birth, sex = EXCLUDED.sex, updated_at = EXCLUDED.updated_at
            """;

    private static final String DELETE_SQL = "DELETE FROM automation.registration_drafts WHERE conversation_id = ?";
    private static final String DELETE_STALE_SQL = "DELETE FROM automation.registration_drafts WHERE updated_at < ?";

    private final JdbcTemplate jdbcTemplate;
    private final FieldCipher cipher;

    public JdbcRegistrationDraftRepository(JdbcTemplate jdbcTemplate, FieldCipher cipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.cipher = cipher;
    }

    @Override
    public Optional<RegistrationDraft> find(UUID conversationId) {
        return jdbcTemplate.query(FIND_SQL, (row, rowNum) -> toDraft(row), conversationId).stream().findFirst();
    }

    @Override
    public void save(RegistrationDraft draft) {
        jdbcTemplate.update(UPSERT_SQL, draft.conversationId(), draft.clinicId(), draft.consentVersion(),
                encrypt(draft.firstName()), encrypt(draft.lastNamePaterno()), encrypt(draft.lastNameMaterno()),
                encrypt(draft.dateOfBirth() == null ? null : draft.dateOfBirth().toString()),
                draft.sex() == null ? null : draft.sex().name(), Timestamp.valueOf(draft.updatedAt()));
    }

    @Override
    public void delete(UUID conversationId) {
        jdbcTemplate.update(DELETE_SQL, conversationId);
    }

    /** Altas abandonadas a medias: no se guardan datos personales mas de lo necesario. */
    public int deleteStale(LocalDateTime before) {
        return jdbcTemplate.update(DELETE_STALE_SQL, Timestamp.valueOf(before));
    }

    private RegistrationDraft toDraft(ResultSet row) throws SQLException {
        String birth = decrypt(row.getString("date_of_birth"));
        String sex = row.getString("sex");
        return new RegistrationDraft(
                row.getObject("conversation_id", UUID.class),
                row.getObject("clinic_id", UUID.class),
                row.getString("consent_version"),
                decrypt(row.getString("first_name")),
                decrypt(row.getString("last_name_paterno")),
                decrypt(row.getString("last_name_materno")),
                birth == null ? null : LocalDate.parse(birth),
                sex == null ? null : Sex.valueOf(sex),
                row.getTimestamp("updated_at").toLocalDateTime());
    }

    private String encrypt(String value) {
        return value == null ? null : cipher.encrypt(value);
    }

    private String decrypt(String value) {
        return value == null ? null : cipher.decrypt(value);
    }
}
