package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.ports.out.ChatContactsPort;
import com.jclinical.core.security.crypto.FieldCipher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Datos de cada numero que escribe: su nombre de perfil de WhatsApp (cifrado) y, de su propio chat,
 * desde cuando escribe, cuantos mensajes van y su primer mensaje (para la senal de enlace).
 */
public class JdbcChatContacts implements ChatContactsPort {

    private static final String UPSERT_SQL = """
            INSERT INTO automation.chat_contacts (clinic_id, phone, profile_name, updated_at) VALUES (?, ?, ?, ?)
            ON CONFLICT (clinic_id, phone) DO UPDATE SET profile_name = EXCLUDED.profile_name, updated_at = EXCLUDED.updated_at
            """;

    private static final String NAMES_SQL =
            "SELECT phone, profile_name FROM automation.chat_contacts WHERE clinic_id = ? AND phone IN (%s)";

    private static final String NAME_SQL =
            "SELECT profile_name FROM automation.chat_contacts WHERE clinic_id = ? AND phone = ?";

    private static final String STATS_SQL = """
            SELECT min(created_at) AS first_at, count(*) AS total
              FROM automation.chat_messages
             WHERE clinic_id = ? AND phone = ?
            """;

    private static final String FIRST_INBOUND_SQL = """
            SELECT body FROM automation.chat_messages
             WHERE clinic_id = ? AND phone = ? AND direction = 'INBOUND'
             ORDER BY created_at
             LIMIT 1
            """;

    private final JdbcTemplate jdbcTemplate;
    private final FieldCipher cipher;

    public JdbcChatContacts(JdbcTemplate jdbcTemplate, FieldCipher cipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.cipher = cipher;
    }

    @Override
    public void saveProfileName(UUID clinicId, String phone, String profileName, LocalDateTime at) {
        jdbcTemplate.update(UPSERT_SQL, clinicId, phone, cipher.encrypt(profileName), Timestamp.valueOf(at));
    }

    @Override
    public Map<String, String> profileNames(UUID clinicId, Collection<String> phones) {
        if (phones.isEmpty()) {
            return Map.of();
        }
        List<Object> arguments = new ArrayList<>();
        arguments.add(clinicId);
        arguments.addAll(phones);
        Map<String, String> names = new HashMap<>();
        jdbcTemplate.query(NAMES_SQL.formatted(String.join(", ", Collections.nCopies(phones.size(), "?"))), row -> {
            String name = row.getString("profile_name");
            if (name != null) {
                names.put(row.getString("phone"), cipher.decrypt(name));
            }
        }, arguments.toArray());
        return Map.copyOf(names);
    }

    @Override
    public Optional<ChatFacts> facts(UUID clinicId, String phone) {
        ChatFacts facts = jdbcTemplate.queryForObject(STATS_SQL, (row, rowNum) -> {
            Timestamp first = row.getTimestamp("first_at");
            return first == null ? null : new ChatFacts(first.toLocalDateTime(), null, row.getInt("total"), null);
        }, clinicId, phone);
        String profileName = jdbcTemplate.query(NAME_SQL, (row, rowNum) -> row.getString("profile_name"), clinicId, phone)
                .stream().findFirst().map(cipher::decrypt).orElse(null);
        if (facts == null) {
            return profileName == null ? Optional.empty() : Optional.of(new ChatFacts(null, null, 0, profileName));
        }
        String firstInbound = jdbcTemplate.query(FIRST_INBOUND_SQL, (row, rowNum) -> row.getString("body"), clinicId, phone)
                .stream().findFirst().map(cipher::decrypt).orElse(null);
        return Optional.of(new ChatFacts(facts.firstMessageAt(), firstInbound, facts.messageCount(), profileName));
    }
}
