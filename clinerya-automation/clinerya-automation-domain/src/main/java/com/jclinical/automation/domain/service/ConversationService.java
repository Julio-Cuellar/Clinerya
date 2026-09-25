package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AvailableSlot;
import com.jclinical.automation.domain.model.Conversation;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.ConversationState;
import com.jclinical.automation.domain.model.InboundMessage;
import com.jclinical.automation.domain.model.OutboundReply;
import com.jclinical.automation.domain.ports.in.HandleInboundMessageUseCase;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort.NewAppointmentRequest;
import com.jclinical.automation.domain.ports.out.AppointmentRequestPort.SlotNoLongerAvailableException;
import com.jclinical.automation.domain.ports.out.ConversationRepositoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort.DoctorContact;
import com.jclinical.automation.domain.ports.out.IntentInterpreterPort;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort;
import com.jclinical.automation.domain.ports.out.PatientDirectoryPort.PatientContact;
import com.jclinical.automation.domain.ports.out.SlotAvailabilityPort;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Maquina de estados de la conversacion de citas (CU-2). Toda transicion sale de una opcion que el
 * propio motor ofrecio en el paso anterior: un boton o fila elegida se valida contra
 * {@link Conversation#offeredOptions()}, y el texto libre solo se traduce, con el interprete, a una
 * de esas mismas opciones. Lo que no encaja se vuelve a preguntar; nunca se improvisa una accion.
 */
public class ConversationService implements HandleInboundMessageUseCase {

    public static final String BOOK = "action:book";
    public static final String LAST_DOCTOR = "action:last-doctor";
    public static final String SHOW_DOCTORS = "action:show-doctors";

    static final String PATIENT_PREFIX = "patient:";
    static final String DOCTOR_PREFIX = "doctor:";
    static final String SLOT_PREFIX = "slot:";

    static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);
    static final int MAX_UNRECOGNIZED = 3;
    static final int SLOT_SEARCH_DAYS = 14;
    /** Tope de filas de una lista de WhatsApp. */
    static final int MAX_OPTIONS = 10;

    private static final Set<String> RESTART_WORDS = Set.of("menu", "inicio", "reiniciar", "empezar de nuevo");
    private static final DateTimeFormatter SLOT_LABEL = DateTimeFormatter.ofPattern("EEE dd/MM HH:mm", Locale.forLanguageTag("es-MX"));

    private final ConversationRepositoryPort conversations;
    private final PatientDirectoryPort patients;
    private final DoctorDirectoryPort doctors;
    private final SlotAvailabilityPort slots;
    private final AppointmentRequestPort requests;
    private final IntentInterpreterPort interpreter;
    private final Clock clock;

    public ConversationService(ConversationRepositoryPort conversations, PatientDirectoryPort patients,
                               DoctorDirectoryPort doctors, SlotAvailabilityPort slots,
                               AppointmentRequestPort requests, IntentInterpreterPort interpreter, Clock clock) {
        this.conversations = conversations;
        this.patients = patients;
        this.doctors = doctors;
        this.slots = slots;
        this.requests = requests;
        this.interpreter = interpreter;
        this.clock = clock;
    }

    @Override
    public List<OutboundReply> handle(InboundMessage message) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<Conversation> active = conversations.findActive(message.clinicId(), message.fromPhone())
                .flatMap(conversation -> expireIfIdle(conversation, now));

        if (active.isEmpty()) {
            return List.of(identify(Conversation.start(message.clinicId(), message.fromPhone(), now), now));
        }

        Conversation conversation = active.get();
        if (conversation.state() == ConversationState.ESPERANDO_MEDICO) {
            save(conversation, conversation.state(), conversation.offeredOptions(), 0, now);
            return List.of(OutboundReply.text("Tu solicitud con " + conversation.doctorName()
                    + " sigue pendiente. Te avisaremos por aquí en cuanto la revise."));
        }
        if (conversation.patientId() != null && isRestart(message.text())) {
            return List.of(showMenu(reset(conversation), now));
        }

        String optionId = resolveOption(conversation, message);
        if (optionId == null) {
            return List.of(unrecognized(conversation, now));
        }
        return List.of(transition(conversation, optionId, now));
    }

    // ---- entrada ----------------------------------------------------------------------------

    /** Una conversacion en espera del medico no vence por inactividad: el medico tiene su propio plazo. */
    private Optional<Conversation> expireIfIdle(Conversation conversation, LocalDateTime now) {
        boolean idle = conversation.state() != ConversationState.ESPERANDO_MEDICO
                && conversation.lastActivityAt().plus(IDLE_TIMEOUT).isBefore(now);
        if (!idle) {
            return Optional.of(conversation);
        }
        save(conversation, ConversationState.EXPIRADA, List.of(), conversation.unrecognizedCount(), conversation.lastActivityAt());
        return Optional.empty();
    }

    private String resolveOption(Conversation conversation, InboundMessage message) {
        if (message.selectedOptionId() != null) {
            return isOffered(conversation, message.selectedOptionId()) ? message.selectedOptionId() : null;
        }
        if (message.text() == null || message.text().isBlank() || conversation.offeredOptions().isEmpty()) {
            return null;
        }
        try {
            return interpreter.interpret(message.text(), conversation.offeredOptions())
                    .filter(optionId -> isOffered(conversation, optionId))
                    .orElse(null);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean isOffered(Conversation conversation, String optionId) {
        return conversation.offeredOptions().stream().anyMatch(option -> option.id().equals(optionId));
    }

    private OutboundReply unrecognized(Conversation conversation, LocalDateTime now) {
        int count = conversation.unrecognizedCount() + 1;
        save(conversation, conversation.state(), conversation.offeredOptions(), count, now);
        String text = count >= MAX_UNRECOGNIZED
                ? "No logré entenderte. Puedes elegir una de las opciones o, si lo prefieres, comunícate con la clínica."
                : "No entendí tu respuesta. Elige una de las opciones:";
        return new OutboundReply(text, conversation.offeredOptions());
    }

    // ---- transiciones -----------------------------------------------------------------------

    private OutboundReply transition(Conversation conversation, String optionId, LocalDateTime now) {
        return switch (conversation.state()) {
            case ELEGIR_PACIENTE -> showMenu(withPatient(conversation, optionId), now);
            case MENU -> offerDoctors(conversation, now, null);
            case ELEGIR_MEDICO -> chooseDoctor(conversation, optionId, now);
            case ELEGIR_CUPO -> submitRequest(conversation, optionId, now);
            default -> unrecognized(conversation, now);
        };
    }

    private OutboundReply identify(Conversation conversation, LocalDateTime now) {
        List<PatientContact> found = patients.findByPhone(conversation.clinicId(), conversation.phone());
        if (found.isEmpty()) {
            save(conversation, ConversationState.CERRADA, List.of(), 0, now);
            return OutboundReply.text("No encontramos un paciente registrado con este número en la clínica. "
                    + "Por ahora, comunícate con la clínica para agendar tu cita.");
        }
        if (found.size() == 1) {
            PatientContact patient = found.get(0);
            return showMenu(copy(conversation, conversation.state(), patient.patientId(), patient.displayName(),
                    null, null, List.of(), 0, now), now);
        }
        List<ConversationOption> options = found.stream().limit(MAX_OPTIONS)
                .map(patient -> new ConversationOption(PATIENT_PREFIX + patient.patientId(), patient.displayName()))
                .toList();
        save(conversation, ConversationState.ELEGIR_PACIENTE, options, 0, now);
        return new OutboundReply("Hola. ¿Para quién es la cita?", options);
    }

    private OutboundReply showMenu(Conversation conversation, LocalDateTime now) {
        List<ConversationOption> options = List.of(new ConversationOption(BOOK, "Agendar una cita"));
        save(conversation, ConversationState.MENU, options, 0, now);
        return new OutboundReply("Hola, " + conversation.patientName() + ". ¿Qué deseas hacer?", options);
    }

    private OutboundReply offerDoctors(Conversation conversation, LocalDateTime now, String prefix) {
        Optional<DoctorContact> last = doctors.lastDoctorOf(conversation.clinicId(), conversation.patientId());
        if (last.isEmpty()) {
            return listDoctors(conversation, now, prefix);
        }
        List<ConversationOption> options = List.of(
                new ConversationOption(LAST_DOCTOR, "Con " + last.get().displayName()),
                new ConversationOption(SHOW_DOCTORS, "Ver médicos de la clínica"));
        save(conversation, ConversationState.ELEGIR_MEDICO, options, 0, now);
        return new OutboundReply(withPrefix(prefix, "¿Con qué médico quieres tu cita?"), options);
    }

    private OutboundReply listDoctors(Conversation conversation, LocalDateTime now, String prefix) {
        List<ConversationOption> options = doctors.listDoctors(conversation.clinicId()).stream().limit(MAX_OPTIONS)
                .map(doctor -> new ConversationOption(DOCTOR_PREFIX + doctor.staffId(), doctor.displayName()))
                .toList();
        if (options.isEmpty()) {
            return showMenuWith(conversation, now, "Por ahora la clínica no tiene médicos disponibles para agendar.");
        }
        save(conversation, ConversationState.ELEGIR_MEDICO, options, 0, now);
        return new OutboundReply(withPrefix(prefix, "Elige al médico:"), options);
    }

    private OutboundReply chooseDoctor(Conversation conversation, String optionId, LocalDateTime now) {
        if (SHOW_DOCTORS.equals(optionId)) {
            return listDoctors(conversation, now, null);
        }
        if (LAST_DOCTOR.equals(optionId)) {
            Optional<DoctorContact> last = doctors.lastDoctorOf(conversation.clinicId(), conversation.patientId());
            if (last.isEmpty()) {
                return listDoctors(conversation, now, null);
            }
            return offerSlots(conversation, last.get().staffId(), last.get().displayName(), now, null);
        }
        UUID doctorId = UUID.fromString(optionId.substring(DOCTOR_PREFIX.length()));
        return offerSlots(conversation, doctorId, labelOf(conversation, optionId), now, null);
    }

    private OutboundReply offerSlots(Conversation conversation, UUID doctorId, String doctorName,
                                     LocalDateTime now, String prefix) {
        List<AvailableSlot> available = slots.availableSlots(conversation.clinicId(), doctorId,
                now.toLocalDate(), SLOT_SEARCH_DAYS, MAX_OPTIONS);
        if (available.isEmpty()) {
            return listDoctors(conversation, now,
                    doctorName + " no tiene horarios disponibles en los próximos " + SLOT_SEARCH_DAYS + " días.");
        }
        List<ConversationOption> options = available.stream()
                .map(slot -> new ConversationOption(slotId(slot), slotLabel(slot)))
                .toList();
        Conversation withDoctor = copy(conversation, ConversationState.ELEGIR_CUPO, conversation.patientId(),
                conversation.patientName(), doctorId, doctorName, options, 0, now);
        conversations.save(withDoctor);
        return new OutboundReply(withPrefix(prefix, "Estos son los horarios disponibles con " + doctorName + ":"), options);
    }

    private OutboundReply submitRequest(Conversation conversation, String optionId, LocalDateTime now) {
        AvailableSlot slot = parseSlot(optionId);
        UUID requestId;
        try {
            requestId = requests.submit(new NewAppointmentRequest(conversation.clinicId(), conversation.id(),
                    conversation.patientId(), conversation.doctorStaffId(), slot.start(), slot.end(), conversation.phone(),
                    conversation.patientName(), conversation.doctorName()));
        } catch (SlotNoLongerAvailableException exception) {
            return offerSlots(conversation, conversation.doctorStaffId(), conversation.doctorName(), now,
                    "Ese horario ya no está disponible.");
        }
        conversations.save(new Conversation(conversation.id(), conversation.clinicId(), conversation.phone(),
                ConversationState.ESPERANDO_MEDICO, conversation.patientId(), conversation.patientName(),
                conversation.doctorStaffId(), conversation.doctorName(), requestId, List.of(), 0,
                conversation.createdAt(), now));
        return OutboundReply.text("Listo. Enviamos tu solicitud para el " + labelOf(conversation, optionId)
                + " a " + conversation.doctorName() + ". Te avisaremos por aquí en cuanto la revise.");
    }

    private OutboundReply showMenuWith(Conversation conversation, LocalDateTime now, String prefix) {
        OutboundReply menu = showMenu(conversation, now);
        return new OutboundReply(prefix + " " + menu.text(), menu.options());
    }

    // ---- utilidades -------------------------------------------------------------------------

    private Conversation withPatient(Conversation conversation, String optionId) {
        UUID patientId = UUID.fromString(optionId.substring(PATIENT_PREFIX.length()));
        return copy(conversation, conversation.state(), patientId, labelOf(conversation, optionId),
                null, null, conversation.offeredOptions(), 0, conversation.lastActivityAt());
    }

    private static Conversation reset(Conversation conversation) {
        return copy(conversation, conversation.state(), conversation.patientId(), conversation.patientName(),
                null, null, List.of(), 0, conversation.lastActivityAt());
    }

    private void save(Conversation conversation, ConversationState state, List<ConversationOption> options,
                      int unrecognizedCount, LocalDateTime now) {
        conversations.save(copy(conversation, state, conversation.patientId(), conversation.patientName(),
                conversation.doctorStaffId(), conversation.doctorName(), options, unrecognizedCount, now));
    }

    private static Conversation copy(Conversation conversation, ConversationState state, UUID patientId, String patientName,
                                     UUID doctorId, String doctorName, List<ConversationOption> options,
                                     int unrecognizedCount, LocalDateTime now) {
        return new Conversation(conversation.id(), conversation.clinicId(), conversation.phone(), state,
                patientId, patientName, doctorId, doctorName, conversation.requestId(), options, unrecognizedCount,
                conversation.createdAt(), now);
    }

    private static String labelOf(Conversation conversation, String optionId) {
        return conversation.offeredOptions().stream()
                .filter(option -> option.id().equals(optionId))
                .map(ConversationOption::label)
                .findFirst()
                .orElse("");
    }

    private static String withPrefix(String prefix, String text) {
        return prefix == null ? text : prefix + " " + text;
    }

    static boolean isRestart(String text) {
        if (text == null) {
            return false;
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{L}\\p{N} ]", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        return RESTART_WORDS.contains(normalized);
    }

    static String slotId(AvailableSlot slot) {
        return SLOT_PREFIX + slot.start() + "|" + slot.end();
    }

    static AvailableSlot parseSlot(String optionId) {
        String[] range = optionId.substring(SLOT_PREFIX.length()).split("\\|");
        return new AvailableSlot(LocalDateTime.parse(range[0]), LocalDateTime.parse(range[1]));
    }

    private static String slotLabel(AvailableSlot slot) {
        String label = SLOT_LABEL.format(slot.start()).replace(".", "");
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }
}
