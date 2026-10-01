package com.jclinical.automation.domain.agent.tools;

import com.jclinical.automation.domain.agent.AgentTool;
import com.jclinical.automation.domain.agent.ToolContext;
import com.jclinical.automation.domain.agent.ToolOutcome;
import com.jclinical.automation.domain.agent.ToolSpec;
import com.jclinical.automation.domain.model.ConversationOption;
import com.jclinical.automation.domain.model.PendingAction;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort.DoctorContact;
import com.jclinical.automation.domain.ports.out.PatientRegistrationPort;
import com.jclinical.automation.domain.ports.out.PatientRegistrationPort.NewPatient;
import com.jclinical.automation.domain.ports.out.PatientRegistrationPort.Sex;
import com.jclinical.automation.domain.ports.out.PendingActionPort;
import com.jclinical.automation.domain.service.NewPatientRegistration;
import com.jclinical.automation.domain.service.SlotLabel;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Registra a un paciente nuevo con los datos que dio en el chat, solo si ya acepto la autorizacion de
 * contacto. Valida igual que el alta de recepcion; el sexo se elige con botones. Si ya habia elegido
 * un horario, deja la cita lista para que la confirme.
 */
public final class RegisterPatientTool implements AgentTool {

    public static final String NAME = "registrar_paciente";
    public static final String SEX_FEMALE = "sexo:femenino";
    public static final String SEX_MALE = "sexo:masculino";
    public static final String SEX_OTHER = "sexo:otro";
    static final List<ConversationOption> SEX_OPTIONS = List.of(new ConversationOption(SEX_FEMALE, "Femenino"),
            new ConversationOption(SEX_MALE, "Masculino"), new ConversationOption(SEX_OTHER, "Otro"));
    static final Duration CONSENT_TTL = Duration.ofHours(2);

    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}");
    private static final Set<String> NO_EMAIL = Set.of("no", "no tengo", "no tengo correo", "ninguno", "sin correo");

    private final PatientRegistrationPort registrations;
    private final PendingActionPort pending;
    private final DoctorDirectoryPort doctors;

    public RegisterPatientTool(PatientRegistrationPort registrations, PendingActionPort pending, DoctorDirectoryPort doctors) {
        this.registrations = registrations;
        this.pending = pending;
        this.doctors = doctors;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME, "Registra como paciente a quien escribe, después de que aceptó la autorización de contacto.",
                List.of(new ToolSpec.Parameter("nombre", "string", "Nombre(s), sin apellidos.", true),
                        new ToolSpec.Parameter("apellidos", "string", "Apellido paterno y materno.", true),
                        new ToolSpec.Parameter("fecha_nacimiento", "string", "Fecha de nacimiento dd/mm/aaaa.", true),
                        new ToolSpec.Parameter("sexo", "string", "Id de la opción elegida (sexo:...) o femenino/masculino/otro.", true),
                        new ToolSpec.Parameter("correo", "string", "Correo electrónico; vacío si no tiene.", false),
                        new ToolSpec.Parameter("horario", "string", "Id del horario (slot:...) si ya eligió uno.", false)));
    }

    @Override
    public ToolOutcome run(ToolContext context, Map<String, Object> arguments) {
        boolean consented = pending.find(context.conversationId())
                .filter(action -> action.kind() == PendingAction.Kind.CONSENT_ACCEPTED)
                .filter(action -> !action.proposedAt().plus(CONSENT_TTL).isBefore(context.now()))
                .isPresent();
        if (!consented) {
            return ToolOutcome.of(Map.of("error", "Falta la autorización de contacto del paciente; envíala con pedir_consentimiento."));
        }
        if (!context.patients().isEmpty()) {
            return ToolOutcome.of(Map.of("error", "Quien escribe ya está registrado como paciente."));
        }
        Optional<String> name = NewPatientRegistration.cleanName(ToolArgs.text(arguments, "nombre"));
        if (name.isEmpty()) {
            return ToolOutcome.of(Map.of("error", "El nombre no es válido; pídelo de nuevo, sin apellidos."));
        }
        List<String> surnames = NewPatientRegistration.splitSurnames(ToolArgs.text(arguments, "apellidos"));
        if (surnames.isEmpty()) {
            return ToolOutcome.of(Map.of("error", "Los apellidos no son válidos; pídelos de nuevo."));
        }
        Optional<LocalDate> birthDate = NewPatientRegistration.parseBirthDate(ToolArgs.text(arguments, "fecha_nacimiento"),
                context.now().toLocalDate());
        if (birthDate.isEmpty()) {
            return ToolOutcome.of(Map.of("error", "La fecha de nacimiento no es válida; pídela como dd/mm/aaaa (por ejemplo 14/03/1990)."));
        }
        Optional<Sex> sex = sex(ToolArgs.text(arguments, "sexo"));
        if (sex.isEmpty()) {
            return ToolOutcome.of(Map.of("error", "Falta el sexo; que lo elija con los botones.")).withOptions(SEX_OPTIONS);
        }
        String email = ToolArgs.text(arguments, "correo");
        if (email.isEmpty() || NO_EMAIL.contains(ToolArgs.normalize(email))) {
            email = null;
        } else if (!EMAIL.matcher(email).matches()) {
            return ToolOutcome.of(Map.of("error", "El correo no es válido; pídelo de nuevo o que diga que no tiene."));
        }

        String paterno = surnames.getFirst();
        String materno = surnames.size() > 1 ? String.join(" ", surnames.subList(1, surnames.size())) : null;
        UUID patientId = registrations.register(new NewPatient(context.clinicId(), name.get(), paterno, materno, birthDate.get(),
                sex.get(), context.phone(), email, registrations.consentText().version()));
        pending.clear(context.conversationId());
        String fullName = name.get() + " " + paterno + (materno == null ? "" : " " + materno);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("paciente_registrado", true);
        content.put("nombre", name.get());
        String slotId = ToolArgs.text(arguments, "horario");
        boolean offered = context.offeredOptions().stream().anyMatch(o -> o.id().equals(slotId) && slotId.startsWith(SlotsTool.OPTION_PREFIX));
        if (!offered) {
            return new ToolOutcome(content, List.of(), List.of(name.get()))
                    .withFallback("¡Listo, " + name.get() + "! Ya quedaste registrado como paciente. ¿Te ayudo a agendar tu cita?");
        }
        SlotsTool.ChosenSlot slot = SlotsTool.parse(slotId);
        String doctorName = doctors.listDoctors(context.clinicId()).stream().filter(d -> d.staffId().equals(slot.doctorId()))
                .map(DoctorContact::displayName).findFirst().orElse("tu médico");
        pending.save(new PendingAction(context.conversationId(), PendingAction.Kind.BOOK, patientId, fullName, slot.doctorId(),
                doctorName, slot.start(), slot.end(), null, context.now()));
        String label = SlotLabel.of(slot.start());
        content.put("resumen", label + " con " + doctorName + " para " + fullName);
        content.put("pide_confirmacion", true);
        return new ToolOutcome(content, ConfirmActionTool.CONFIRMATION_OPTIONS, List.of(label, doctorName, fullName))
                .withFallback("¡Listo, " + name.get() + "! Ya quedaste registrado. ¿Confirmo tu cita del " + label + " con "
                        + doctorName + "?");
    }

    private static Optional<Sex> sex(String requested) {
        String value = ToolArgs.normalize(requested);
        return switch (value) {
            case SEX_FEMALE, "femenino", "femenina", "mujer", "f" -> Optional.of(Sex.FEMALE);
            case SEX_MALE, "masculino", "masculina", "hombre", "m" -> Optional.of(Sex.MALE);
            case SEX_OTHER, "otro", "otra", "no binario" -> Optional.of(Sex.OTHER);
            default -> Optional.empty();
        };
    }
}
