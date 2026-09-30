package com.jclinical.automation.infra.adapters.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.automation.domain.model.DoctorNotice;
import com.jclinical.automation.domain.model.PatientNotification;
import com.jclinical.automation.domain.ports.out.DoctorNoticeQueuePort;
import com.jclinical.automation.domain.ports.out.OutboundMessageQueuePort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Cola de mensajes al paciente. Se escribe en la misma transaccion que mueve la conversacion, asi
 * que un mensaje nunca queda sin su cambio de estado (ni al reves). El despachador los envia despues.
 * Si hubiera que usar plantilla, el texto completo va como su unico parametro, salvo que el mensaje traiga
 * su propia plantilla con sus parametros (el recordatorio de cita).
 */
public class JdbcOutboundMessageQueue implements OutboundMessageQueuePort, DoctorNoticeQueuePort {

    private static final String INSERT_SQL = """
            INSERT INTO automation.outbound_messages
                (id, clinic_id, phone, audience, body, options, template_parameters, template_name, status, attempts,
                 next_attempt_at, created_at)
            VALUES (?, ?, ?, 'PATIENT', ?, ?, ?, ?, 'PENDING', 0, ?, ?)
            """;

    private static final String INSERT_DOCTOR_SQL = """
            INSERT INTO automation.outbound_messages
                (id, clinic_id, phone, audience, body, options, template_parameters, status, attempts, next_attempt_at, created_at)
            VALUES (?, ?, ?, 'DOCTOR', ?, '[]', ?, 'PENDING', 0, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ConversationOptionsCodec optionsCodec;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JdbcOutboundMessageQueue(JdbcTemplate jdbcTemplate, ConversationOptionsCodec optionsCodec,
                                    ObjectMapper objectMapper, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.optionsCodec = optionsCodec;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void enqueue(PatientNotification notification) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now(clock));
        jdbcTemplate.update(INSERT_SQL, UUID.randomUUID(), notification.clinicId(), notification.phone(),
                notification.reply().text(), optionsCodec.encode(notification.reply().options()),
                json(notification.templateParameters().isEmpty() ? List.of(notification.reply().text())
                        : notification.templateParameters()),
                notification.templateName(), now, now);
    }

    /** Aviso al medico: el despachador usa la plantilla de medicos si la ventana de 24 h esta cerrada. */
    @Override
    public void enqueue(DoctorNotice notice) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now(clock));
        jdbcTemplate.update(INSERT_DOCTOR_SQL, UUID.randomUUID(), notice.clinicId(), notice.phone(), notice.text(),
                json(notice.templateParameters()), now, now);
    }

    private String json(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudieron guardar los parametros de la plantilla.", exception);
        }
    }
}
