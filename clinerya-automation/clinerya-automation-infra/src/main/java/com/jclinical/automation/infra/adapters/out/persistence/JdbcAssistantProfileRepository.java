package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.model.AssistantProfile;
import com.jclinical.automation.domain.ports.out.AssistantProfilePort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

/** Perfil del asistente guardado junto a la configuracion del canal; sin configurar, perfil vacio. */
public class JdbcAssistantProfileRepository implements AssistantProfilePort {

    private static final String FIND_SQL = """
            SELECT assistant_name, assistant_faq, show_prices
              FROM automation.clinic_channel_settings
             WHERE clinic_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcAssistantProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AssistantProfile find(UUID clinicId) {
        return jdbcTemplate.query(FIND_SQL, (row, rowNum) -> new AssistantProfile(
                        row.getString("assistant_name"), row.getString("assistant_faq"), row.getBoolean("show_prices")),
                        clinicId)
                .stream()
                .findFirst()
                .orElse(AssistantProfile.EMPTY);
    }
}
