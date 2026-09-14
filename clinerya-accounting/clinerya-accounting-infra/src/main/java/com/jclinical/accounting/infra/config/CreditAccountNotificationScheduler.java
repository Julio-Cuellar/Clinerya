package com.jclinical.accounting.infra.config;

import com.jclinical.accounting.domain.model.CreditAccountAlert;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase;
import com.jclinical.core.notifications.NotificationPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreditAccountNotificationScheduler {

    private final JdbcTemplate jdbcTemplate;
    private final ManageOpeningBalancesUseCase openingBalancesUseCase;
    private final NotificationPublisherPort notificationPublisher;

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT30S")
    public void publishCreditAlerts() {
        try {
            List<UUID> clinicIds = jdbcTemplate.query(
                    "SELECT id FROM clinics.clinics",
                    (resultSet, rowNum) -> resultSet.getObject("id", UUID.class)
            );
            LocalDate today = LocalDate.now();
            clinicIds.forEach(clinicId -> openingBalancesUseCase
                    .listCreditAccountAlerts(clinicId, today, 7)
                    .forEach(alert -> publish(clinicId, alert)));
        } catch (Exception exception) {
            log.error("Error publicando notificaciones de tarjetas de crédito", exception);
        }
    }

    private void publish(UUID clinicId, CreditAccountAlert alert) {
        notificationPublisher.publish(new NotificationPublisherPort.NotificationCommand(
                clinicId,
                "CONTABILIDAD",
                "CREDIT_ACCOUNT_" + alert.accountId() + "_" + alert.type() + "_" + alert.alertDate(),
                alert.severity().toUpperCase(),
                alert.title(),
                alert.detail(),
                "/contabilidad/bancos/" + alert.accountId(),
                null
        ));
    }
}
