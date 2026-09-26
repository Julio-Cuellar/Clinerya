package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.ports.out.PatientRegistrationPort;
import com.jclinical.automation.domain.ports.out.RegistrationDraftPort;

import java.time.Clock;

public class NewPatientRegistration {

    public static final String ACCEPT = "action:accept-consent";
    public static final String DECLINE = "action:decline-consent";
    public static final String NO_EMAIL = "action:no-email";

    public NewPatientRegistration(ConversationRepositoryPort conversations, PatientRegistrationPort registrations,
                                  RegistrationDraftPort drafts, Clock clock) {
    }
}
