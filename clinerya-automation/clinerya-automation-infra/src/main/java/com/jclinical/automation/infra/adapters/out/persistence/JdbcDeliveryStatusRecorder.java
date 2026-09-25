package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.model.DeliveryStatusUpdate;
import com.jclinical.automation.domain.ports.out.DeliveryStatusPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

/**
 * Estado de entrega de lo enviado. Meta puede avisar fuera de orden; el estado solo avanza
 * (enviado < entregado < leido) y una falla se registra siempre, con su codigo.
 */
public class JdbcDeliveryStatusRecorder implements DeliveryStatusPort {

    private static final String UPDATE_SQL = """
            UPDATE automation.outbound_messages
               SET delivery_status = ?, last_error = COALESCE(?, last_error)
             WHERE clinic_id = ? AND wa_message_id = ?
               AND (delivery_status IS NULL OR ? = 'FAILED'
                    OR (CASE delivery_status WHEN 'SENT' THEN 1 WHEN 'DELIVERED' THEN 2 WHEN 'READ' THEN 3 ELSE 0 END)
                     < (CASE ? WHEN 'SENT' THEN 1 WHEN 'DELIVERED' THEN 2 WHEN 'READ' THEN 3 ELSE 0 END))
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcDeliveryStatusRecorder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(UUID clinicId, DeliveryStatusUpdate update) {
        String status = update.status().name();
        String error = update.status() == DeliveryStatusUpdate.Status.FAILED
                ? "Meta reportó que no se pudo entregar" + (update.errorCode() == null ? "" : " (código " + update.errorCode() + ")")
                : null;
        jdbcTemplate.update(UPDATE_SQL, status, error, clinicId, update.waMessageId(), status, status);
    }
}
