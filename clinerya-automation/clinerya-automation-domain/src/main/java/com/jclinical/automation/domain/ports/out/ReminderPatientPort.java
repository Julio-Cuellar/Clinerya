package com.jclinical.automation.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

/** A quien se le recuerda la cita: su nombre, su celular y si autorizo que le escribamos por WhatsApp. */
@FunctionalInterface
public interface ReminderPatientPort {

    Optional<ReminderRecipient> find(UUID clinicId, UUID patientId);

    record ReminderRecipient(String fullName, String phone, boolean whatsappConsent) {}
}
