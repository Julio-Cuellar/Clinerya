package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.ConversationState;
import com.jclinical.automation.domain.model.InboundMessage;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.model.RegistrationDraft;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.ports.out.PatientRegistrationPort;
import com.jclinical.automation.domain.ports.out.PatientRegistrationPort.ConsentText;
import com.jclinical.automation.domain.ports.out.PatientRegistrationPort.NewPatient;
import com.jclinical.automation.domain.ports.out.RegistrationDraftPort;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CU-4: alta por WhatsApp de alguien que aun no es paciente. Primero autoriza el contacto (texto
 * versionado del modulo de pacientes y aviso de privacidad); luego da nombre, apellidos, fecha de
 * nacimiento y, si quiere, correo. Lo que escribe se guarda en un borrador hasta registrarlo. Estos
 * datos personales se validan aqui y nunca se mandan al interprete (Gemini).
 */
public class NewPatientRegistration {

    public static final String ACCEPT = "action:accept-consent";
    public static final String DECLINE = "action:decline-consent";
    public static final String NO_EMAIL = "action:no-email";

    static final int MAX_NAME_LENGTH = 60;
    static final int MAX_AGE_YEARS = 120;

    private static final Set<ConversationState> STATES = EnumSet.of(ConversationState.REGISTRO_CONSENTIMIENTO,
            ConversationState.REGISTRO_NOMBRE, ConversationState.REGISTRO_APELLIDOS,
            ConversationState.REGISTRO_NACIMIENTO, ConversationState.REGISTRO_CORREO);
    private static final List<ConversationOption> CONSENT_OPTIONS = List.of(
            new ConversationOption(ACCEPT, "Acepto"), new ConversationOption(DECLINE, "No acepto"));
    private static final List<ConversationOption> EMAIL_OPTIONS = List.of(new ConversationOption(NO_EMAIL, "No tengo correo"));
    /** Particulas que forman parte del apellido siguiente: "de la Cruz", "del Valle". */
    private static final Set<String> SURNAME_PARTICLES = Set.of("de", "del", "la", "las", "los", "y", "san", "santa");
    private static final Set<String> NO_EMAIL_WORDS = Set.of("no", "no tengo", "no tengo correo", "ninguno", "sin correo");

    private static final Pattern NAME = Pattern.compile("\\p{L}[\\p{L} .'-]*");
    private static final Pattern BIRTH_DATE = Pattern.compile("(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})");
    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}");

    static final String ASK_NAME = "¿Cuál es tu nombre? (sin apellidos)";
    static final String ASK_LAST_NAMES = "¿Cuáles son tus apellidos?";
    static final String DATE_FORMAT_HINT = "Escríbela así: dd/mm/aaaa, por ejemplo 14/03/1990.";
    static final String ASK_BIRTH_DATE = "¿Cuál es tu fecha de nacimiento? " + DATE_FORMAT_HINT;
    static final String ASK_EMAIL = "¿Cuál es tu correo electrónico? Si no tienes, elige \"No tengo correo\".";

    /** Resultado de un mensaje durante el alta. */
    public sealed interface Step permits Reply, Registered, Declined {}

    /** Sigue el alta: esta es la siguiente pregunta (o la misma, si el dato no era valido). */
    public record Reply(OutboundReply reply) implements Step {}

    /** Quedo registrado: la conversacion ya trae al paciente; toca elegir medico. */
    public record Registered(Conversation conversation, String firstName) implements Step {}

    /** No autorizo el contacto: no se guardo nada. */
    public record Declined() implements Step {}

    private final ConversationRepositoryPort conversations;
    private final PatientRegistrationPort registrations;
    private final RegistrationDraftPort drafts;
    private final Clock clock;

    public NewPatientRegistration(ConversationRepositoryPort conversations, PatientRegistrationPort registrations,
                                  RegistrationDraftPort drafts, Clock clock) {
        this.conversations = conversations;
        this.registrations = registrations;
        this.drafts = drafts;
        this.clock = clock;
    }

    public boolean handles(ConversationState state) {
        return STATES.contains(state);
    }

    /** Primer paso: la autorizacion de contacto, con el aviso de privacidad si la clinica lo tiene. */
    public OutboundReply start(Conversation conversation, String privacyNoticeUrl) {
        ConsentText consent = registrations.consentText();
        move(conversation, ConversationState.REGISTRO_CONSENTIMIENTO, CONSENT_OPTIONS);
        String privacy = privacyNoticeUrl == null || privacyNoticeUrl.isBlank()
                ? "" : "\nAviso de privacidad: " + privacyNoticeUrl.trim();
        return new OutboundReply("Para agendar primero te registramos como paciente. " + consent.text() + privacy,
                CONSENT_OPTIONS);
    }

    public Step onMessage(Conversation conversation, InboundMessage message) {
        return switch (conversation.state()) {
            case REGISTRO_CONSENTIMIENTO -> onConsent(conversation, message);
            case REGISTRO_NOMBRE -> onFirstName(conversation, message.text());
            case REGISTRO_APELLIDOS -> onLastNames(conversation, message.text());
            case REGISTRO_NACIMIENTO -> onBirthDate(conversation, message.text());
            case REGISTRO_CORREO -> onEmail(conversation, message);
            default -> throw new IllegalStateException("La conversación no está en el alta: " + conversation.state());
        };
    }

    /** Reiniciar a mitad del alta descarta lo capturado. */
    public void discard(UUID conversationId) {
        drafts.delete(conversationId);
    }

    // ---- pasos --------------------------------------------------------------------------------

    private Step onConsent(Conversation conversation, InboundMessage message) {
        if (DECLINE.equals(message.selectedOptionId())) {
            drafts.delete(conversation.id());
            return new Declined();
        }
        if (!ACCEPT.equals(message.selectedOptionId())) {
            return new Reply(new OutboundReply("Para continuar elige \"Acepto\" o \"No acepto\".", CONSENT_OPTIONS));
        }
        drafts.save(new RegistrationDraft(conversation.id(), conversation.clinicId(),
                registrations.consentText().version(), null, null, null, null, now()));
        return ask(conversation, ConversationState.REGISTRO_NOMBRE, "Gracias. " + ASK_NAME, List.of());
    }

    private Step onFirstName(Conversation conversation, String text) {
        Optional<String> name = cleanName(text);
        if (name.isEmpty()) {
            return new Reply(OutboundReply.text("Escríbeme solo tu nombre, por ejemplo: Juan."));
        }
        RegistrationDraft draft = draftOf(conversation);
        drafts.save(new RegistrationDraft(draft.conversationId(), draft.clinicId(), draft.consentVersion(), name.get(),
                null, null, null, now()));
        return ask(conversation, ConversationState.REGISTRO_APELLIDOS, ASK_LAST_NAMES, List.of());
    }

    private Step onLastNames(Conversation conversation, String text) {
        List<String> surnames = splitSurnames(text);
        if (surnames.isEmpty()) {
            return new Reply(OutboundReply.text("Escríbeme tus apellidos, por ejemplo: Pérez López."));
        }
        RegistrationDraft draft = draftOf(conversation);
        String materno = surnames.size() > 1 ? String.join(" ", surnames.subList(1, surnames.size())) : null;
        drafts.save(new RegistrationDraft(draft.conversationId(), draft.clinicId(), draft.consentVersion(),
                draft.firstName(), surnames.get(0), materno, null, now()));
        return ask(conversation, ConversationState.REGISTRO_NACIMIENTO, ASK_BIRTH_DATE, List.of());
    }

    private Step onBirthDate(Conversation conversation, String text) {
        Optional<LocalDate> birthDate = parseBirthDate(text, now().toLocalDate());
        if (birthDate.isEmpty()) {
            return new Reply(OutboundReply.text("No reconocí esa fecha. " + DATE_FORMAT_HINT));
        }
        RegistrationDraft draft = draftOf(conversation);
        drafts.save(new RegistrationDraft(draft.conversationId(), draft.clinicId(), draft.consentVersion(),
                draft.firstName(), draft.lastNamePaterno(), draft.lastNameMaterno(), birthDate.get(), now()));
        return ask(conversation, ConversationState.REGISTRO_CORREO, ASK_EMAIL, EMAIL_OPTIONS);
    }

    private Step onEmail(Conversation conversation, InboundMessage message) {
        String text = message.text() == null ? "" : message.text().trim();
        if (NO_EMAIL.equals(message.selectedOptionId()) || NO_EMAIL_WORDS.contains(text.toLowerCase(Locale.ROOT))) {
            return register(conversation, null);
        }
        if (!EMAIL.matcher(text).matches()) {
            return new Reply(new OutboundReply("Ese correo no parece válido. Escríbelo completo, por ejemplo "
                    + "nombre@correo.com, o elige \"No tengo correo\".", EMAIL_OPTIONS));
        }
        return register(conversation, text.toLowerCase(Locale.ROOT));
    }

    private Step register(Conversation conversation, String email) {
        RegistrationDraft draft = draftOf(conversation);
        UUID patientId = registrations.register(new NewPatient(draft.clinicId(), draft.firstName(),
                draft.lastNamePaterno(), draft.lastNameMaterno(), draft.dateOfBirth(), conversation.phone(), email,
                draft.consentVersion()));
        drafts.delete(conversation.id());
        Conversation withPatient = new Conversation(conversation.id(), conversation.clinicId(), conversation.phone(),
                conversation.state(), patientId, draft.firstName() + " " + draft.lastNamePaterno(), null, null,
                conversation.requestId(), List.of(), 0, conversation.createdAt(), now());
        return new Registered(withPatient, draft.firstName());
    }

    // ---- validacion ---------------------------------------------------------------------------

    static Optional<String> cleanName(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String clean = text.trim().replaceAll("\\s+", " ");
        if (clean.isEmpty() || clean.length() > MAX_NAME_LENGTH || !NAME.matcher(clean).matches()) {
            return Optional.empty();
        }
        return Optional.of(clean);
    }

    /** Apellidos en orden; las particulas se unen al apellido que sigue ("de la Cruz"). */
    static List<String> splitSurnames(String text) {
        Optional<String> clean = cleanName(text);
        if (clean.isEmpty()) {
            return List.of();
        }
        List<String> surnames = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : clean.get().split(" ")) {
            current.append(current.isEmpty() ? "" : " ").append(word);
            if (!SURNAME_PARTICLES.contains(word.toLowerCase(Locale.ROOT))) {
                surnames.add(current.toString());
                current.setLength(0);
            }
        }
        if (!current.isEmpty()) {
            surnames.add(current.toString());
        }
        return surnames;
    }

    /** dd/mm/aaaa (tambien con - o .); una fecha que existe, no futura y de hace menos de 120 anos. */
    static Optional<LocalDate> parseBirthDate(String text, LocalDate today) {
        if (text == null) {
            return Optional.empty();
        }
        Matcher matcher = BIRTH_DATE.matcher(text.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            LocalDate date = LocalDate.of(Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(1)));
            boolean plausible = !date.isAfter(today) && date.isAfter(today.minusYears(MAX_AGE_YEARS));
            return plausible ? Optional.of(date) : Optional.empty();
        } catch (DateTimeException impossible) {
            return Optional.empty();
        }
    }

    // ---- utilidades -------------------------------------------------------------------------

    private RegistrationDraft draftOf(Conversation conversation) {
        return drafts.find(conversation.id())
                .orElseThrow(() -> new IllegalStateException("El alta perdió sus datos; vuelve a empezar con \"menu\"."));
    }

    private Reply ask(Conversation conversation, ConversationState next, String question, List<ConversationOption> options) {
        move(conversation, next, options);
        return new Reply(new OutboundReply(question, options));
    }

    private void move(Conversation conversation, ConversationState state, List<ConversationOption> options) {
        conversations.save(new Conversation(conversation.id(), conversation.clinicId(), conversation.phone(), state,
                conversation.patientId(), conversation.patientName(), conversation.doctorStaffId(),
                conversation.doctorName(), conversation.requestId(), options, 0, conversation.createdAt(), now()));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
