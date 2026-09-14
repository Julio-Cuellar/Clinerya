package com.jclinical.app.dev;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jclinical.accounting.domain.model.BankAccount;
import com.jclinical.accounting.domain.model.BankAccountType;
import com.jclinical.accounting.domain.model.OpeningBalanceSetup;
import com.jclinical.accounting.domain.model.OperationalAccountKind;
import com.jclinical.accounting.domain.ports.in.ManageOpeningBalancesUseCase;
import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.ports.in.ManageClinicScheduleUseCase;
import com.jclinical.attachments.AttachmentService;
import com.jclinical.cash.domain.model.PaymentMethod;
import com.jclinical.cash.domain.ports.in.ManageCashExpensesUseCase;
import com.jclinical.cash.domain.ports.in.ManageCashSessionUseCase;
import com.jclinical.cash.domain.ports.in.ManageTicketsUseCase;
import com.jclinical.clinics.domain.model.Clinic;
import com.jclinical.staff.domain.model.StaffRole;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import com.jclinical.clinics.domain.ports.in.ManageClinicUseCase;
import com.jclinical.inventory.domain.model.Material;
import com.jclinical.inventory.domain.model.MovementType;
import com.jclinical.inventory.domain.model.PurchaseOrder;
import com.jclinical.inventory.domain.model.Supplier;
import com.jclinical.inventory.domain.ports.in.ManageInventoryMovementUseCase;
import com.jclinical.inventory.domain.ports.in.ManageMaterialUseCase;
import com.jclinical.inventory.domain.ports.in.ManagePurchasingUseCase;
import com.jclinical.patients.domain.model.Address;
import com.jclinical.patients.domain.model.BloodType;
import com.jclinical.patients.domain.model.EmergencyContact;
import com.jclinical.patients.domain.model.Gender;
import com.jclinical.patients.domain.model.MaritalStatus;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.RegisterPatientUseCase;
import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.DiagnosisKind;
import com.jclinical.records.domain.model.MedicalHistoryTemplate;
import com.jclinical.records.domain.model.NoteStatus;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase;
import com.jclinical.records.domain.ports.in.ManageHistoryTemplateUseCase;
import com.jclinical.records.domain.ports.in.ManageMedicalHistoryUseCase;
import com.jclinical.records.domain.ports.in.ManagePrivacyConsentUseCase;
import com.jclinical.treatments.domain.model.ItemProgressStatus;
import com.jclinical.treatments.domain.model.Quotation;
import com.jclinical.treatments.domain.model.QuotationStatus;
import com.jclinical.treatments.domain.model.TreatmentCatalogItem;
import com.jclinical.treatments.domain.ports.in.ManageQuotationUseCase;
import com.jclinical.treatments.domain.ports.in.ManageTreatmentCatalogUseCase;
import com.jclinical.treatments.domain.ports.in.ManageVisitsUseCase;
import com.jclinical.users.domain.model.User;
import com.jclinical.users.domain.model.UserPreRegistration;
import com.jclinical.users.domain.ports.in.RegisterUserUseCase;
import com.jclinical.users.domain.ports.in.VerifyUserEmailUseCase;
import com.jclinical.users.domain.ports.out.UserPreRegistrationRepositoryPort;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds a second, isolated development account with a connected end-to-end data set.
 * The account is intentionally skipped after its first successful creation.
 */
@Component
@ConditionalOnProperty(name = "app.showcase-seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
@Order(110)
public class ShowcaseDataSeeder implements ApplicationRunner {

    private static final String STUDIES_ELEMENT_ID = "patient_studies";
    private static final String USER_AGENT = "Clinerya showcase seeder";
    private static final String IP_ADDRESS = "127.0.0.1";

    private final RegisterUserUseCase registerUserUseCase;
    private final VerifyUserEmailUseCase verifyUserEmailUseCase;
    private final UserPreRegistrationRepositoryPort preRegistrationRepository;
    private final UserRepositoryPort userRepository;
    private final ManageClinicUseCase clinicUseCase;
    private final ManageClinicStaffUseCase clinicStaffUseCase;
    private final ManageClinicScheduleUseCase clinicScheduleUseCase;
    private final ManageOpeningBalancesUseCase openingBalancesUseCase;
    private final ManageMaterialUseCase materialUseCase;
    private final ManageInventoryMovementUseCase movementUseCase;
    private final ManagePurchasingUseCase purchasingUseCase;
    private final RegisterPatientUseCase registerPatientUseCase;
    private final ManageHistoryTemplateUseCase templateUseCase;
    private final ManageMedicalHistoryUseCase historyUseCase;
    private final ManageClinicalNoteUseCase clinicalNoteUseCase;
    private final ManagePrivacyConsentUseCase privacyConsentUseCase;
    private final AttachmentService attachmentService;
    private final ManageTreatmentCatalogUseCase catalogUseCase;
    private final ManageQuotationUseCase quotationUseCase;
    private final ManageVisitsUseCase visitsUseCase;
    private final ManageAppointmentsUseCase appointmentsUseCase;
    private final ManageCashSessionUseCase cashSessionUseCase;
    private final ManageTicketsUseCase ticketsUseCase;
    private final ManageCashExpensesUseCase cashExpensesUseCase;
    private final ObjectMapper objectMapper;

    @Value("${app.showcase-seed.email}")
    private String email;

    @Value("${app.showcase-seed.password}")
    private String password;

    @Value("${app.showcase-seed.full-name}")
    private String fullName;

    @Value("${app.showcase-seed.clinic-name}")
    private String clinicName;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(email)) {
            log.info(">>>> [SHOWCASE-SEED] La cuenta '{}' ya existe; no se duplican sus datos.", email);
            logCredentials();
            return;
        }

        User user = createVerifiedUser();
        Clinic clinic = clinicUseCase.getClinicsByOwner(user.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se aprovisiono la clinica de la cuenta showcase."));
        UUID clinicId = clinic.getId();

        clinicUseCase.updateClinic(
                user.getId(),
                clinicId,
                clinicName,
                "Clinica Dental Aurora S.A. de C.V.",
                "CDA260101AB1",
                "601",
                "Av. Juarez 210, Consultorio 4",
                "Centro",
                "Puebla",
                "Puebla",
                "72000",
                "222 555 0184",
                email,
                null,
                "America/Mexico_City",
                "/aviso-privacidad",
                "263300201A0123",
                "Dra. Aurora Morales",
                "12345678",
                3
        );

        UUID staffId = clinicStaffUseCase.getActiveStaffByUserAndClinic(user.getId(), clinicId)
                .map(ManageClinicStaffUseCase.StaffSummary::staffId)
                .orElseThrow(() -> new IllegalStateException("No se encontro el perfil medico de la cuenta showcase."));

        seedSchedule(clinicId);
        BankAccount operatingBank = seedAccounting(user.getId(), clinicId);
        InventoryFixture inventory = seedInventoryAndPurchasing(user.getId(), clinicId);
        PatientFixture patients = seedPatients(clinicId);
        seedPersonnel(clinicId);
        seedRecords(clinicId, user, staffId, patients);
        TreatmentFixture treatments = seedTreatments(clinicId, user, staffId, patients, inventory);
        seedAgenda(clinicId, staffId, patients, treatments);
        seedCash(clinicId, user.getId(), staffId, operatingBank, patients, treatments);

        log.info(">>>> [SHOWCASE-SEED] Muestra integral creada: 3 pacientes, expediente versionado, 2 estudios, "
                + "4 tratamientos, 3 citas, inventario, proveedores, compras, caja y contabilidad.");
        logCredentials();
    }

    private User createVerifiedUser() {
        registerUserUseCase.registerUser(new RegisterUserUseCase.RegisterUserCommand(
                email, password, fullName, clinicName));
        UserPreRegistration preRegistration = preRegistrationRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("No se encontro el pre-registro showcase."));
        verifyUserEmailUseCase.verifyEmail(email, preRegistration.getVerificationToken());
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("No se encontro el usuario showcase verificado."));
    }

    private void seedSchedule(UUID clinicId) {
        List<ManageClinicScheduleUseCase.DayScheduleCommand> schedule = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            boolean open = !DayOfWeek.SUNDAY.equals(day);
            schedule.add(new ManageClinicScheduleUseCase.DayScheduleCommand(
                    day,
                    open,
                    open ? LocalTime.of(8, 0) : null,
                    open ? LocalTime.of(20, 0) : null
            ));
        }
        clinicScheduleUseCase.updateSchedule(clinicId, schedule);
    }

    private BankAccount seedAccounting(UUID actingUserId, UUID clinicId) {
        OpeningBalanceSetup setup = openingBalancesUseCase.configureOpeningBalances(
                actingUserId,
                clinicId,
                new ManageOpeningBalancesUseCase.ConfigureOpeningBalancesCommand(
                        LocalDate.now().minusDays(90),
                        money("2500"),
                        money("18000"),
                        List.of(
                                new ManageOpeningBalancesUseCase.BankAccountOpeningCommand(
                                        "BBVA", "Cuenta operativa", "8473", "MXN", money("32000"), BankAccountType.DEBIT, OperationalAccountKind.BANK),
                                new ManageOpeningBalancesUseCase.BankAccountOpeningCommand(
                                        "Santander", "Tarjeta empresarial", "4830", "MXN", BigDecimal.ZERO, BankAccountType.CREDIT, OperationalAccountKind.BANK)
                        ),
                        "Apertura contable del consultorio para demostracion"
                )
        );
        return setup.getBankAccounts().stream()
                .filter(account -> account.getAccountType() == BankAccountType.DEBIT)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se creo la cuenta bancaria operativa."));
    }

    private InventoryFixture seedInventoryAndPurchasing(UUID actingUserId, UUID clinicId) {
        Material gloves = createMaterial(actingUserId, clinicId, "Guantes de nitrilo talla M", "Desechables", "DES-001",
                "Ambiderm", "Guante de exploracion sin latex", "par", "Caja 100", money("100"),
                money("0.95"), money("200"), false, null, false);
        registerMovement(actingUserId, clinicId, gloves.getId(), MovementType.PURCHASE_ENTRY, null, money("4"), null, null,
                "Existencia inicial: 4 cajas");

        Material lidocaine = createMaterial(actingUserId, clinicId, "Lidocaina 2% con epinefrina", "Anestesicos", "ANE-010",
                "Septodont", "Cartucho para anestesia local", "cartucho", null, null,
                money("7.20"), money("30"), false, null, true);
        registerMovement(actingUserId, clinicId, lidocaine.getId(), MovementType.PURCHASE_ENTRY, money("60"), null,
                "LIDO-2606", LocalDate.now().plusMonths(11), "Existencia inicial por lote");

        Material resin = createMaterial(actingUserId, clinicId, "Resina fotocurable A2", "Restaurativos", "RES-A2",
                "3M", "Resina universal tono A2", "jeringa", null, null,
                money("420"), money("5"), false, null, true);
        registerMovement(actingUserId, clinicId, resin.getId(), MovementType.PURCHASE_ENTRY, money("12"), null,
                "RES-A2-26", LocalDate.now().plusMonths(16), "Existencia inicial por lote");

        Material needles = createMaterial(actingUserId, clinicId, "Aguja dental corta 30G", "Anestesicos", "AGU-30G",
                "Terumo", "Aguja esteril desechable", "pieza", "Caja 100", money("100"),
                money("1.80"), money("100"), false, null, false);
        registerMovement(actingUserId, clinicId, needles.getId(), MovementType.PURCHASE_ENTRY, null, money("2"), null, null,
                "Existencia inicial: 2 cajas");

        Material masks = createMaterial(actingUserId, clinicId, "Cubrebocas tricapa", "Desechables", "DES-020",
                "SafeMask", "Cubrebocas desechable de tres capas", "pieza", "Caja 50", money("50"),
                money("0.60"), money("200"), true, money("3.00"), false);
        registerMovement(actingUserId, clinicId, masks.getId(), MovementType.PURCHASE_ENTRY, null, money("6"), null, null,
                "Existencia inicial: 6 cajas");
        registerMovement(actingUserId, clinicId, masks.getId(), MovementType.ADJUSTMENT_OUT, money("5"), null, null, null,
                "Merma documentada por empaque danado");

        Supplier dentalDepot = purchasingUseCase.createSupplier(actingUserId, clinicId,
                new ManagePurchasingUseCase.CreateSupplierCommand(
                        "Dental Depot Puebla", "Laura Gomez", "222 410 8820", "ventas@dentaldepot.example",
                        "DDP190101AA1", "Entrega martes y jueves"));
        Supplier odontomed = purchasingUseCase.createSupplier(actingUserId, clinicId,
                new ManagePurchasingUseCase.CreateSupplierCommand(
                        "Distribuidora Odontomed", "Carlos Rivera", "222 903 1460", "pedidos@odontomed.example",
                        "ODO180420BC2", "Credito autorizado a 30 dias"));

        purchasingUseCase.addSupplierMaterial(actingUserId, clinicId, dentalDepot.getId(),
                new ManagePurchasingUseCase.AddSupplierMaterialCommand(gloves.getId(), money("0.90")));
        purchasingUseCase.addSupplierMaterial(actingUserId, clinicId, dentalDepot.getId(),
                new ManagePurchasingUseCase.AddSupplierMaterialCommand(lidocaine.getId(), money("6.95")));
        purchasingUseCase.addSupplierMaterial(actingUserId, clinicId, odontomed.getId(),
                new ManagePurchasingUseCase.AddSupplierMaterialCommand(resin.getId(), money("405")));
        purchasingUseCase.addSupplierMaterial(actingUserId, clinicId, odontomed.getId(),
                new ManagePurchasingUseCase.AddSupplierMaterialCommand(masks.getId(), money("0.55")));

        PurchaseOrder receivedOrder = purchasingUseCase.createPurchaseOrder(actingUserId, clinicId,
                new ManagePurchasingUseCase.CreatePurchaseOrderCommand(
                        dentalDepot.getId(), LocalDate.now().minusDays(12), LocalDate.now().minusDays(8),
                        "Reposicion mensual de anestesia y desechables",
                        null,
                        List.of(
                                new ManagePurchasingUseCase.CreatePurchaseOrderLineCommand(gloves.getId(), money("200"), money("0.90")),
                                new ManagePurchasingUseCase.CreatePurchaseOrderLineCommand(lidocaine.getId(), money("40"), money("6.95"))
                        )));
        purchasingUseCase.markPurchaseOrderOrdered(actingUserId, clinicId, receivedOrder.getId());
        purchasingUseCase.receivePurchaseOrder(actingUserId, clinicId, receivedOrder.getId(),
                new ManagePurchasingUseCase.ReceivePurchaseOrderCommand(
                        LocalDateTime.now().minusDays(8),
                        "Mercancia recibida completa y validada",
                        List.of(
                                new ManagePurchasingUseCase.ReceivePurchaseOrderLineCommand(
                                        receivedOrder.getLines().get(0).getId(), money("200"), money("0.90"), null, null),
                                new ManagePurchasingUseCase.ReceivePurchaseOrderLineCommand(
                                        receivedOrder.getLines().get(1).getId(), money("40"), money("6.95"),
                                        "LIDO-2607", LocalDate.now().plusMonths(14))
                        )));

        PurchaseOrder pendingOrder = purchasingUseCase.createPurchaseOrder(actingUserId, clinicId,
                new ManagePurchasingUseCase.CreatePurchaseOrderCommand(
                        odontomed.getId(), LocalDate.now(), LocalDate.now().plusDays(4),
                        "Pedido en camino para tratamientos restaurativos",
                        null,
                        List.of(
                                new ManagePurchasingUseCase.CreatePurchaseOrderLineCommand(resin.getId(), money("6"), money("405")),
                                new ManagePurchasingUseCase.CreatePurchaseOrderLineCommand(masks.getId(), money("250"), money("0.55"))
                        )));
        purchasingUseCase.markPurchaseOrderOrdered(actingUserId, clinicId, pendingOrder.getId());

        return new InventoryFixture(gloves, lidocaine, resin, needles, masks);
    }

    private PatientFixture seedPatients(UUID clinicId) {
        Patient primary = registerPatientUseCase.registerPatient(new RegisterPatientUseCase.RegisterPatientCommand(
                clinicId, "Mariana", "Torres", "Vega", "TOVM880414MPLRGR08", LocalDate.of(1988, 4, 14),
                Gender.FEMALE, "2225551024", "mariana.torres@example.com", "Arquitecta", MaritalStatus.MARRIED,
                "Mexicana", BloodType.A_POSITIVE,
                address("5 Oriente", "408", "3", "Centro", "Puebla", "Puebla", "72000"),
                emergency("Alejandro Torres", "Esposo", "2225551099")));

        Patient pediatric = registerPatientUseCase.registerPatient(new RegisterPatientUseCase.RegisterPatientCommand(
                clinicId, "Santiago", "Ramirez", "Cruz", "RACS170922HPLMNR01", LocalDate.of(2017, 9, 22),
                Gender.MALE, "2225552077", "familia.ramirez@example.com", "Estudiante", MaritalStatus.SINGLE,
                "Mexicana", BloodType.O_POSITIVE,
                address("31 Poniente", "1208", null, "Volcanes", "Puebla", "Puebla", "72410"),
                emergency("Daniela Cruz", "Madre", "2225552088")));

        Patient geriatric = registerPatientUseCase.registerPatient(new RegisterPatientUseCase.RegisterPatientCommand(
                clinicId, "Roberto", "Hernandez", "Diaz", "HEDR520305HPLRZB02", LocalDate.of(1952, 3, 5),
                Gender.MALE, "2225553090", "roberto.hernandez@example.com", "Jubilado", MaritalStatus.WIDOWED,
                "Mexicana", BloodType.B_POSITIVE,
                address("Camino Real", "910", null, "Cholula", "San Andres Cholula", "Puebla", "72810"),
                emergency("Lucia Hernandez", "Hija", "2225553190")));

        return new PatientFixture(primary, pediatric, geriatric);
    }

    private void seedPersonnel(UUID clinicId) {
        clinicStaffUseCase.inviteStaff(clinicId, "recepcion.aurora@example.com", StaffRole.RECEPTIONIST);
        clinicStaffUseCase.inviteStaff(clinicId, "asistente.aurora@example.com", StaffRole.ASSISTANT);
    }

    private void seedRecords(UUID clinicId, User user, UUID staffId, PatientFixture patients) {
        MedicalHistoryTemplate odontology = templateUseCase.createTemplate(clinicId,
                new ManageHistoryTemplateUseCase.CreateTemplateCommand(
                        "Historia clinica odontologica (NOM-013)",
                        "Plantilla dental integral con odontograma, consentimientos y contrato de servicios.",
                        buildOdontologySchema()));
        templateUseCase.createTemplate(clinicId,
                new ManageHistoryTemplateUseCase.CreateTemplateCommand(
                        "Historia clinica odontopediatrica",
                        "Valoracion para pacientes pediatricos, tutor y habitos orales.",
                        buildSimpleSchema("odontopediatrica", List.of(
                                field("nombreTutor", "ficha_identificacion", "Nombre del padre, madre o tutor", "text", 16, 16, 868, 56),
                                field("motivoConsultaPediatrica", "padecimiento_actual", "Motivo de consulta", "textarea", 16, 90, 868, 110),
                                field("habitosOralesPediatricos", "antecedentes_no_patologicos", "Habitos orales", "textarea", 16, 220, 868, 110)
                        ))));
        templateUseCase.createTemplate(clinicId,
                new ManageHistoryTemplateUseCase.CreateTemplateCommand(
                        "Historia clinica geriatrica odontologica",
                        "Valoracion dental para adulto mayor, medicacion y apoyo del cuidador.",
                        buildSimpleSchema("geriatrica", List.of(
                                field("cuidadorPrincipal", "ficha_identificacion", "Cuidador o contacto principal", "text", 16, 16, 868, 56),
                                field("medicacionGeriatrica", "antecedentes_patologicos", "Medicacion y enfermedades cronicas", "textarea", 16, 90, 868, 130),
                                field("planGeriatrico", "indicacion_terapeutica", "Plan de atencion", "textarea", 16, 240, 868, 120)
                        ))));

        String patientSignature = signatureDataUrl("Mariana T.");
        String doctorSignature = signatureDataUrl("Dra. Elena M.");
        Map<String, String> versionOne = historyAnswers(patients.primary(), patientSignature, doctorSignature, false);
        historyUseCase.saveMedicalHistory(
                patients.primary().getId(), clinicId,
                new ManageMedicalHistoryUseCase.SaveHistoryCommand(
                        odontology.getId(), writeJson(versionOne), fullName, IP_ADDRESS, USER_AGENT),
                user.getId());

        Map<String, String> versionTwo = historyAnswers(patients.primary(), patientSignature, doctorSignature, true);
        historyUseCase.saveMedicalHistory(
                patients.primary().getId(), clinicId,
                new ManageMedicalHistoryUseCase.SaveHistoryCommand(
                        odontology.getId(), writeJson(versionTwo), fullName, IP_ADDRESS, USER_AGENT),
                user.getId());

        ClinicalNote note = clinicalNoteUseCase.createClinicalNote(
                patients.primary().getId(), clinicId, staffId,
                new ManageClinicalNoteUseCase.CreateNoteCommand(
                        "Sensibilidad al frio en molar inferior derecho desde hace cinco dias.",
                        "Caries profunda en pieza 46; respuesta prolongada a prueba termica.",
                        null, null, null, null, null, null, null,
                        "Pulpitis irreversible sintomatica en pieza 46.",
                        "Endodoncia de pieza 46 y restauracion definitiva posterior.",
                        NoteStatus.DRAFT),
                user.getId());
        clinicalNoteUseCase.signClinicalNote(
                note.getId(), patients.primary().getId(), clinicId, user.getId(),
                new ManageClinicalNoteUseCase.SignNoteCommand(fullName, IP_ADDRESS, USER_AGENT,
                        List.of(new ManageClinicalNoteUseCase.DiagnosisEntry("K04.0", DiagnosisKind.PRIMARY))));

        clinicalNoteUseCase.createClinicalNote(
                patients.pediatric().getId(), clinicId, staffId,
                new ManageClinicalNoteUseCase.CreateNoteCommand(
                        "Acude a valoracion preventiva acompanado por su madre.",
                        "Denticion mixta, higiene regular, sin dolor actual.",
                        null, null, null, null, null, null, null,
                        "Riesgo moderado de caries.",
                        "Profilaxis, tecnica de cepillado y control en seis meses.",
                        NoteStatus.DRAFT),
                user.getId());

        privacyConsentUseCase.saveConsent(
                patients.primary().getId(), clinicId,
                new ManagePrivacyConsentUseCase.SignConsentCommand(
                        "Autorizo a Clinica Dental Aurora el tratamiento de mis datos personales sensibles de salud "
                                + "y el uso de Clinerya para integrar y conservar mi expediente clinico.",
                        "Mariana Torres Vega", patientSignature, IP_ADDRESS, USER_AGENT),
                user.getId());

        attachmentService.store(clinicId, patients.primary().getId(), STUDIES_ELEMENT_ID,
                new MemoryMultipartFile("radiografia-panoramica-demo.pdf", "application/pdf",
                        simplePdf("Radiografia panoramica", "Paciente: Mariana Torres Vega", "Hallazgo demo: pieza 46 en valoracion endodontica.")));
        attachmentService.store(clinicId, patients.primary().getId(), STUDIES_ELEMENT_ID,
                new MemoryMultipartFile("biometria-hematica-demo.pdf", "application/pdf",
                        simplePdf("Biometria hematica", "Paciente: Mariana Torres Vega", "Resultados demostrativos dentro de parametros de referencia.")));
    }

    private TreatmentFixture seedTreatments(
            UUID clinicId,
            User user,
            UUID staffId,
            PatientFixture patients,
            InventoryFixture inventory) {
        TreatmentCatalogItem cleaning = catalogUseCase.createCatalogItem(user.getId(), clinicId,
                new ManageTreatmentCatalogUseCase.CreateCatalogItemCommand(
                        "Limpieza dental y profilaxis", "PREVENTIVE", "Limpieza con ultrasonido y pulido.",
                        money("650"), 45,
                        List.of(new ManageTreatmentCatalogUseCase.CatalogMaterialCommand(inventory.gloves().getId(), BigDecimal.ONE))));
        TreatmentCatalogItem filling = catalogUseCase.createCatalogItem(user.getId(), clinicId,
                new ManageTreatmentCatalogUseCase.CreateCatalogItemCommand(
                        "Resina molar", "RESTORATIVE", "Restauracion fotocurable en molar.",
                        money("1200"), 50,
                        List.of(
                                new ManageTreatmentCatalogUseCase.CatalogMaterialCommand(inventory.gloves().getId(), BigDecimal.ONE),
                                new ManageTreatmentCatalogUseCase.CatalogMaterialCommand(inventory.resin().getId(), money("0.20"))
                        )));
        TreatmentCatalogItem rootCanal = catalogUseCase.createCatalogItem(user.getId(), clinicId,
                new ManageTreatmentCatalogUseCase.CreateCatalogItemCommand(
                        "Endodoncia molar", "ENDODONTICS", "Tratamiento de conductos en pieza molar.",
                        money("3200"), 90,
                        List.of(
                                new ManageTreatmentCatalogUseCase.CatalogMaterialCommand(inventory.gloves().getId(), money("2")),
                                new ManageTreatmentCatalogUseCase.CatalogMaterialCommand(inventory.lidocaine().getId(), money("2")),
                                new ManageTreatmentCatalogUseCase.CatalogMaterialCommand(inventory.needles().getId(), BigDecimal.ONE)
                        )));
        TreatmentCatalogItem consultation = catalogUseCase.createCatalogItem(user.getId(), clinicId,
                new ManageTreatmentCatalogUseCase.CreateCatalogItemCommand(
                        "Consulta de valoracion", "DIAGNOSTIC", "Valoracion clinica y plan inicial.",
                        money("500"), 30, List.of()));

        Quotation primaryQuote = quotationUseCase.createQuotation(
                patients.primary().getId(), clinicId, user.getId(),
                new ManageQuotationUseCase.CreateQuotationCommand(
                        LocalDate.now().minusDays(7),
                        "Plan integral por etapas. Incluye control posterior.", LocalDate.now().plusDays(23),
                        List.of(
                                quotationItem(cleaning, null, inventory.gloves(), BigDecimal.ONE),
                                quotationItem(filling, 36, inventory.gloves(), BigDecimal.ONE, inventory.resin(), money("0.20")),
                                quotationItem(rootCanal, 46,
                                        inventory.gloves(), money("2"), inventory.lidocaine(), money("2"),
                                        inventory.needles(), BigDecimal.ONE)
                        )));
        quotationUseCase.transitionStatus(primaryQuote.getId(), patients.primary().getId(), clinicId, user.getId(), QuotationStatus.SENT);
        primaryQuote = quotationUseCase.transitionStatus(
                primaryQuote.getId(), patients.primary().getId(), clinicId, user.getId(), QuotationStatus.ACCEPTED);

        Quotation pediatricQuote = quotationUseCase.createQuotation(
                patients.pediatric().getId(), clinicId, user.getId(),
                new ManageQuotationUseCase.CreateQuotationCommand(
                        LocalDate.now(), "Valoracion y prevencion pediatrica.", LocalDate.now().plusDays(30),
                        List.of(new ManageQuotationUseCase.QuotationItemCommand(
                                consultation.getId(), consultation.getName(), null, consultation.getDefaultPrice(), List.of(), BigDecimal.ZERO))));
        quotationUseCase.transitionStatus(pediatricQuote.getId(), patients.pediatric().getId(), clinicId, user.getId(), QuotationStatus.SENT);

        quotationUseCase.createQuotation(
                patients.geriatric().getId(), clinicId, user.getId(),
                new ManageQuotationUseCase.CreateQuotationCommand(
                        LocalDate.now(), "Borrador pendiente de revisar con familiar responsable.", LocalDate.now().plusDays(30),
                        List.of(quotationItem(cleaning, null, inventory.gloves(), BigDecimal.ONE))));

        visitsUseCase.registerVisit(
                patients.primary().getId(), primaryQuote.getId(), clinicId, user.getId(),
                new ManageVisitsUseCase.RegisterVisitCommand(
                        LocalDate.now().minusDays(4), staffId, "Profilaxis completada sin incidencias.",
                        List.of(new ManageVisitsUseCase.RegisterVisitLineItemCommand(
                                primaryQuote.getItems().get(0).getId(),
                                List.of(new ManageVisitsUseCase.RegisterVisitMaterialUsageCommand(
                                        inventory.gloves().getId(), inventory.gloves().getName(), BigDecimal.ONE))))));
        quotationUseCase.updateItemProgress(
                primaryQuote.getId(), primaryQuote.getItems().get(0).getId(), patients.primary().getId(), clinicId, user.getId(),
                ItemProgressStatus.COMPLETED);

        return new TreatmentFixture(primaryQuote, pediatricQuote);
    }

    private void seedAgenda(
            UUID clinicId,
            UUID staffId,
            PatientFixture patients,
            TreatmentFixture treatments) {
        LocalDate pastDate = clinicDay(LocalDate.now(), -1);
        Appointment completed = appointmentsUseCase.createAppointment(clinicId,
                new ManageAppointmentsUseCase.CreateAppointmentCommand(
                        patients.primary().getId(), staffId, null, null,
                        pastDate.atTime(10, 0), pastDate.atTime(10, 45),
                        "Profilaxis dental", "Cita completada de demostracion"));
        appointmentsUseCase.transitionStatus(completed.getId(), clinicId, AppointmentStatus.COMPLETED);

        LocalDate treatmentDate = clinicDay(LocalDate.now(), 1);
        Appointment confirmed = appointmentsUseCase.createAppointment(clinicId,
                new ManageAppointmentsUseCase.CreateAppointmentCommand(
                        patients.primary().getId(), staffId,
                        treatments.primaryQuote().getId(), treatments.primaryQuote().getItems().get(2).getId(),
                        treatmentDate.atTime(11, 0), treatmentDate.atTime(12, 30),
                        "Endodoncia de pieza 46", "Material reservado automaticamente desde la cotizacion"));
        appointmentsUseCase.transitionStatus(confirmed.getId(), clinicId, AppointmentStatus.CONFIRMED);

        LocalDate pediatricDate = clinicDay(LocalDate.now(), 3);
        appointmentsUseCase.createAppointment(clinicId,
                new ManageAppointmentsUseCase.CreateAppointmentCommand(
                        patients.pediatric().getId(), staffId, null, null,
                        pediatricDate.atTime(16, 0), pediatricDate.atTime(16, 30),
                        "Valoracion odontopediatrica", "Acude acompanado por su madre"));
    }

    private void seedCash(
            UUID clinicId,
            UUID actingUserId,
            UUID staffId,
            BankAccount operatingBank,
            PatientFixture patients,
            TreatmentFixture treatments) {
        cashSessionUseCase.openSession(actingUserId, clinicId,
                new ManageCashSessionUseCase.OpenSessionCommand(staffId, money("1500")));
        BigDecimal quoteTotal = treatments.primaryQuote().grandTotal();
        BigDecimal cashAmount = money("1200");
        BigDecimal transferAmount = quoteTotal.subtract(cashAmount);
        ticketsUseCase.registerTicket(actingUserId, clinicId,
                new ManageTicketsUseCase.RegisterTicketCommand(
                        patients.primary().getId(), treatments.primaryQuote().getId(),
                        "Liquidacion de plan integral odontologico", staffId,
                        List.of(
                                new ManageTicketsUseCase.PaymentLineCommand(PaymentMethod.CASH, cashAmount, null, null),
                                new ManageTicketsUseCase.PaymentLineCommand(
                                        PaymentMethod.TRANSFER, transferAmount, "SPEI-DEMO-1842", operatingBank.getId())
                        ),
                        BigDecimal.ZERO, null, null));
        cashExpensesUseCase.registerExpense(actingUserId, clinicId,
                new ManageCashExpensesUseCase.RegisterExpenseCommand(
                        "Papeleria y articulos de limpieza", money("150"), staffId));
        cashSessionUseCase.closeSession(actingUserId, clinicId,
                new ManageCashSessionUseCase.CloseSessionCommand(staffId, money("2570")));

        cashSessionUseCase.openSession(actingUserId, clinicId,
                new ManageCashSessionUseCase.OpenSessionCommand(staffId, money("1000")));
        ticketsUseCase.registerTicket(actingUserId, clinicId,
                new ManageTicketsUseCase.RegisterTicketCommand(
                        patients.pediatric().getId(), null, "Consulta de valoracion odontopediatrica", staffId,
                        List.of(new ManageTicketsUseCase.PaymentLineCommand(
                                PaymentMethod.CARD, money("500"), "TPV-DEMO-7315", operatingBank.getId())),
                        BigDecimal.ZERO, null, null));
    }

    private Material createMaterial(
            UUID actingUserId,
            UUID clinicId,
            String name,
            String category,
            String code,
            String brand,
            String description,
            String unit,
            String presentation,
            BigDecimal quantityPerPresentation,
            BigDecimal unitCost,
            BigDecimal minimumStock,
            boolean saleEnabled,
            BigDecimal salePrice,
            boolean tracksBatches) {
        return materialUseCase.createMaterial(actingUserId, clinicId, new ManageMaterialUseCase.CreateMaterialCommand(
                name, category, code, brand, description, unit, presentation, quantityPerPresentation,
                unitCost, minimumStock, saleEnabled, salePrice, tracksBatches));
    }

    private void registerMovement(
            UUID actingUserId,
            UUID clinicId,
            UUID materialId,
            MovementType type,
            BigDecimal quantity,
            BigDecimal presentationQuantity,
            String lotNumber,
            LocalDate expirationDate,
            String notes) {
        ManageInventoryMovementUseCase.RegisterMovementCommand command =
                new ManageInventoryMovementUseCase.RegisterMovementCommand(
                        type, quantity, presentationQuantity, null, notes, lotNumber, expirationDate, null);
        switch (type) {
            case PURCHASE_ENTRY -> movementUseCase.registerPurchaseEntry(actingUserId, clinicId, materialId, command);
            case ADJUSTMENT_IN, ADJUSTMENT_OUT -> movementUseCase.registerAdjustment(actingUserId, clinicId, materialId, command);
            case SALE_EXIT -> movementUseCase.registerSaleExit(actingUserId, clinicId, materialId, command);
            case USAGE_EXIT -> throw new IllegalArgumentException("Las salidas por uso requieren una visita clinica.");
        }
    }

    private ManageQuotationUseCase.QuotationItemCommand quotationItem(
            TreatmentCatalogItem item,
            Integer tooth,
            Object... materialQuantityPairs) {
        if (materialQuantityPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Cada material debe incluir su cantidad.");
        }
        List<ManageQuotationUseCase.MaterialLineCommand> materials = new ArrayList<>();
        for (int i = 0; i < materialQuantityPairs.length; i += 2) {
            Material material = (Material) materialQuantityPairs[i];
            BigDecimal quantity = (BigDecimal) materialQuantityPairs[i + 1];
            materials.add(new ManageQuotationUseCase.MaterialLineCommand(
                    material.getId(), material.getName(), quantity, material.getUnitCost()));
        }
        return new ManageQuotationUseCase.QuotationItemCommand(
                item.getId(), item.getName(), tooth, item.getDefaultPrice(), materials, BigDecimal.ZERO);
    }

    private Map<String, String> historyAnswers(
            Patient patient,
            String patientSignature,
            String doctorSignature,
            boolean updated) {
        String odontogram = writeJson(Map.of("teeth", Map.of(
                "46", Map.of("surfaces", Map.of("O", "caries", "M", "caries"), "whole", "sano"),
                "36", Map.of("surfaces", Map.of("O", "obturado"), "whole", "sano"),
                "44", Map.of("surfaces", Map.of(), "whole", "ausente"),
                "14", Map.of("surfaces", updated ? Map.of("D", "caries") : Map.of(), "whole", "sano")
        )));
        int decayed = updated ? 2 : 1;
        int total = updated ? 4 : 3;

        Map<String, String> answers = new LinkedHashMap<>();
        answers.put("nombre", patient.getFirstName() + " " + patient.getLastNamePaterno() + " " + patient.getLastNameMaterno());
        answers.put("sexo", "Femenino");
        answers.put("edad", "38");
        answers.put("domicilio", "5 Oriente 408 Int. 3, Centro, Puebla, Puebla, C.P. 72000");
        answers.put("fechaNacimiento", "1988-04-14");
        answers.put("telefono", patient.getPhone());
        answers.put("ocupacion", patient.getOccupation());
        answers.put("estadoCivil", "Casada");
        answers.put("tipoSangreRh", "A+");
        answers.put("alergias", writeJson(List.of(List.of("Penicilina", "Urticaria"))));
        answers.put("antecedentesHeredofamiliaresDental",
                writeJson(List.of(List.of("Hipertension arterial", "Si", "Padre bajo control medico"))));
        answers.put("antecedentesPatologicosDental",
                writeJson(List.of(List.of("Alergia a penicilina", "Si", "Evitar betalactamicos"))));
        answers.put("habitosDentales", writeJson(List.of(
                List.of("Rechina los dientes", "No", ""),
                List.of("Se muerde las unas", "Si", "Ocasional"))));
        answers.put("padecimientoActualDental", updated
                ? "Sensibilidad persistente y dolor nocturno localizado en pieza 46."
                : "Sensibilidad al frio y dolor intermitente en pieza 46.");
        answers.put("diagnosticoDental", updated
                ? "Pulpitis irreversible sintomatica en pieza 46 y caries incipiente distal en pieza 14."
                : "Pulpitis irreversible sintomatica en pieza 46.");
        answers.put("planTratamientoDental", "Profilaxis, endodoncia de pieza 46 y restauracion con resina en pieza 36.");
        answers.put("pronosticoDental", "Favorable con apego al plan de tratamiento y controles periodicos.");
        answers.put("odontogramaDental", odontogram);
        answers.put("indiceCpodDental", writeJson(List.of(
                List.of("Cariados", String.valueOf(decayed), ""),
                List.of("Perdidos", "1", ""),
                List.of("Obturados", "1", ""),
                List.of("Total CPOD", String.valueOf(total), "")
        )));
        answers.put("observacionesOdontogramaDental", "Pieza 46 prioritaria; pieza 44 ausente y pieza 36 restaurada.");
        answers.put("consentimientoTratamientoPaciente", "Mariana Torres Vega");
        answers.put("consentimientoTratamientoFecha", LocalDate.now().toString());
        answers.put("consentimientoTratamientoCiudad", "Puebla, Puebla");
        answers.put("consentimientoTratamientoProcedimiento", "Endodoncia de pieza 46 y restauraciones indicadas.");
        answers.put("consentimientoTratamientoPronostico", "Favorable, sujeto a evolucion clinica.");
        answers.put("contratoServiciosPaciente", "Mariana Torres Vega");
        answers.put("contratoServiciosCiudad", "Puebla, Puebla");
        answers.put("contratoServiciosFecha", LocalDate.now().toString());
        answers.put("consentimientoAppFirmante", "Mariana Torres Vega");
        answers.put("consentimientoAppCaracter", "Paciente");
        answers.put("consentimientoAppFecha", LocalDate.now().toString());
        answers.put("consentimientoAppAvisoPrivacidad", "Version 2026.1");
        answers.put("firmaDeclaracionPaciente", patientSignature);
        answers.put("firmaDeclaracionDoctor", doctorSignature);
        answers.put("firmaConsentimientoTratamientoPaciente", patientSignature);
        answers.put("firmaConsentimientoTratamientoDoctor", doctorSignature);
        answers.put("firmaContratoPaciente", patientSignature);
        answers.put("firmaContratoDoctor", doctorSignature);
        answers.put("firmaConsentimientoAppPaciente", patientSignature);
        answers.put("firmaConsentimientoAppClinica", doctorSignature);
        return answers;
    }

    private String buildOdontologySchema() {
        List<Map<String, Object>> identification = List.of(
                field("nombre", "ficha_identificacion", "Nombre completo", "text", 16, 16, 430, 56),
                selectField("sexo", "ficha_identificacion", "Sexo", 466, 16, 418, 56, List.of("Masculino", "Femenino")),
                field("edad", "ficha_identificacion", "Edad", "number", 16, 86, 280, 56),
                field("fechaNacimiento", "ficha_identificacion", "Fecha de nacimiento", "date", 316, 86, 280, 56),
                field("telefono", "ficha_identificacion", "Telefono", "text", 616, 86, 268, 56),
                field("domicilio", "ficha_identificacion", "Domicilio", "text", 16, 156, 868, 56),
                field("ocupacion", "ficha_identificacion", "Ocupacion", "text", 16, 226, 280, 56),
                field("estadoCivil", "ficha_identificacion", "Estado civil", "text", 316, 226, 280, 56),
                field("tipoSangreRh", "ficha_identificacion", "Tipo de sangre y RH", "text", 616, 226, 268, 56),
                tableField("alergias", "ficha_identificacion", "Alergias", 16, 296, 868, 140,
                        List.of("Alergeno", "Tipo de reaccion"))
        );
        List<Map<String, Object>> history = List.of(
                tableField("antecedentesHeredofamiliaresDental", null, "Antecedentes heredofamiliares", 16, 16, 868, 170,
                        List.of("Padecimiento", "Si/No", "Observaciones")),
                tableField("antecedentesPatologicosDental", null, "Antecedentes personales patologicos", 16, 205, 868, 170,
                        List.of("Padecimiento", "Si/No", "Observaciones")),
                tableField("habitosDentales", null, "Habitos orales", 16, 394, 868, 190,
                        List.of("Habito", "Si/No", "Observaciones")),
                field("padecimientoActualDental", null, "Padecimiento actual", "textarea", 16, 603, 868, 120)
        );
        List<Map<String, Object>> systems = List.of(
                tableField("sistemaCardiovascularDental", null, "Sistema cardiovascular", 16, 16, 424, 190,
                        List.of("Dato clinico", "Si/No", "Observaciones")),
                tableField("sistemaEndocrinoDental", null, "Sistema endocrino", 460, 16, 424, 190,
                        List.of("Dato clinico", "Si/No", "Observaciones")),
                tableField("sistemaRespiratorioDental", null, "Sistema respiratorio", 16, 225, 424, 190,
                        List.of("Dato clinico", "Si/No", "Observaciones")),
                tableField("sistemaDigestivoDental", null, "Sistema digestivo", 460, 225, 424, 190,
                        List.of("Dato clinico", "Si/No", "Observaciones")),
                field("informacionAdicionalSaludDental", null, "Informacion adicional de salud", "textarea", 16, 434, 868, 110)
        );
        List<Map<String, Object>> dental = List.of(
                field("diagnosticoDental", null, "Diagnostico", "textarea", 16, 16, 280, 110),
                field("planTratamientoDental", null, "Plan de tratamiento", "textarea", 316, 16, 280, 110),
                field("pronosticoDental", null, "Pronostico", "textarea", 616, 16, 268, 110),
                field("odontogramaDental", null, "Odontograma", "odontogram", 16, 146, 868, 320),
                tableField("indiceCpodDental", null, "Indice CPOD", 16, 486, 424, 160,
                        List.of("Indicador", "Inicial", "Final")),
                field("observacionesOdontogramaDental", null, "Observaciones del odontograma", "textarea", 460, 486, 424, 160),
                field("firmaDeclaracionPaciente", null, "Firma del paciente", "signature_patient", 16, 666, 424, 120),
                field("firmaDeclaracionDoctor", null, "Firma del medico", "signature_doctor", 460, 666, 424, 120)
        );
        List<Map<String, Object>> treatmentConsent = List.of(
                field("consentimientoTratamientoOdontologicoTexto", null,
                        "Consentimiento informado para tratamientos odontologicos, intervenciones quirurgicas y procedimientos especiales",
                        "textarea", 16, 16, 868, 250),
                field("consentimientoTratamientoPaciente", null, "Nombre del paciente", "text", 16, 286, 424, 56),
                field("consentimientoTratamientoFecha", null, "Fecha", "date", 460, 286, 424, 56),
                field("consentimientoTratamientoProcedimiento", null, "Procedimiento autorizado", "textarea", 16, 362, 424, 110),
                field("consentimientoTratamientoPronostico", null, "Pronostico y riesgos", "textarea", 460, 362, 424, 110),
                field("firmaConsentimientoTratamientoPaciente", null, "Firma del paciente", "signature_patient", 16, 502, 424, 120),
                field("firmaConsentimientoTratamientoDoctor", null, "Firma del medico", "signature_doctor", 460, 502, 424, 120)
        );
        List<Map<String, Object>> contract = List.of(
                field("contratoServiciosOdontologicosTexto", null,
                        "Contrato de adhesion para la prestacion de servicios odontologicos", "textarea", 16, 16, 868, 350),
                field("contratoServiciosPaciente", null, "Nombre del paciente", "text", 16, 386, 424, 56),
                field("contratoServiciosFecha", null, "Fecha", "date", 460, 386, 424, 56),
                field("firmaContratoPaciente", null, "Firma del paciente", "signature_patient", 16, 472, 424, 120),
                field("firmaContratoDoctor", null, "Firma del prestador", "signature_doctor", 460, 472, 424, 120)
        );
        List<Map<String, Object>> appConsent = List.of(
                field("consentimientoAppInformacionTexto", "consentimiento_expreso",
                        "Consentimiento informado sobre el uso de Clinerya y tratamiento de la informacion",
                        "textarea", 16, 16, 868, 340),
                field("consentimientoAppFirmante", "consentimiento_expreso", "Nombre del firmante", "text", 16, 376, 424, 56),
                field("consentimientoAppCaracter", "consentimiento_expreso", "Caracter con el que firma", "text", 460, 376, 424, 56),
                field("consentimientoAppFecha", "consentimiento_expreso", "Fecha", "date", 16, 452, 424, 56),
                field("consentimientoAppAvisoPrivacidad", "consentimiento_expreso", "Version del aviso de privacidad", "text", 460, 452, 424, 56),
                field("firmaConsentimientoAppPaciente", "consentimiento_expreso", "Firma del paciente", "signature_patient", 16, 538, 424, 120),
                field("firmaConsentimientoAppClinica", "consentimiento_expreso", "Firma de la clinica", "signature_doctor", 460, 538, 424, 120)
        );

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("kind", "historia_clinica");
        schema.put("pages", List.of(
                page("identificacion", identification),
                page("antecedentes", history),
                page("sistemas", systems),
                page("odontograma", dental),
                page("consentimiento_tratamiento", treatmentConsent),
                page("contrato_adhesion", contract),
                page("consentimiento_app", appConsent)
        ));
        return writeJson(schema);
    }

    private String buildSimpleSchema(String id, List<Map<String, Object>> elements) {
        return writeJson(Map.of(
                "kind", "historia_clinica",
                "pages", List.of(page(id, elements))
        ));
    }

    private Map<String, Object> page(String id, List<Map<String, Object>> elements) {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("id", "showcase_" + id);
        page.put("elements", elements);
        page.put("canvasHeight", 1215);
        return page;
    }

    private Map<String, Object> field(
            String id,
            String sectionId,
            String label,
            String type,
            int x,
            int y,
            int width,
            int height) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", id);
        if (sectionId != null) {
            field.put("sectionId", sectionId);
        }
        field.put("label", label);
        field.put("type", type);
        field.put("x", x);
        field.put("y", y);
        field.put("width", width);
        field.put("height", height);
        field.put("fontFamily", "helvetica");
        field.put("fontSize", 12);
        field.put("align", "left");
        return field;
    }

    private Map<String, Object> selectField(
            String id,
            String sectionId,
            String label,
            int x,
            int y,
            int width,
            int height,
            List<String> options) {
        Map<String, Object> field = field(id, sectionId, label, "select", x, y, width, height);
        field.put("options", options);
        return field;
    }

    private Map<String, Object> tableField(
            String id,
            String sectionId,
            String label,
            int x,
            int y,
            int width,
            int height,
            List<String> columns) {
        Map<String, Object> field = field(id, sectionId, label, "table", x, y, width, height);
        field.put("columns", columns);
        return field;
    }

    private Address address(
            String street,
            String outdoorNumber,
            String indoorNumber,
            String colonia,
            String municipality,
            String state,
            String zipCode) {
        return Address.builder()
                .street(street)
                .outdoorNumber(outdoorNumber)
                .indoorNumber(indoorNumber)
                .colonia(colonia)
                .municipality(municipality)
                .state(state)
                .zipCode(zipCode)
                .build();
    }

    private EmergencyContact emergency(String name, String relationship, String phone) {
        return EmergencyContact.builder().fullName(name).relationship(relationship).phone(phone).build();
    }

    private LocalDate clinicDay(LocalDate start, int offset) {
        int direction = offset < 0 ? -1 : 1;
        int remaining = Math.abs(offset);
        LocalDate cursor = start;
        while (remaining > 0) {
            cursor = cursor.plusDays(direction);
            if (!DayOfWeek.SUNDAY.equals(cursor.getDayOfWeek())) {
                remaining--;
            }
        }
        return cursor;
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo construir la informacion showcase.", exception);
        }
    }

    private String signatureDataUrl(String signer) {
        try {
            BufferedImage image = new BufferedImage(520, 120, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(new Color(30, 45, 55));
            graphics.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.drawLine(45, 83, 470, 83);
            graphics.setFont(new Font("Serif", Font.ITALIC, 32));
            graphics.drawString(signer, 90, 72);
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar la firma showcase.", exception);
        }
    }

    private byte[] simplePdf(String title, String subtitle, String detail) {
        String content = "BT /F1 18 Tf 72 720 Td (" + pdfText(title) + ") Tj "
                + "0 -34 Td /F1 11 Tf (" + pdfText(subtitle) + ") Tj "
                + "0 -24 Td (" + pdfText(detail) + ") Tj ET";
        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + content.getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n" + content + "\nendstream"
        );
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.US_ASCII).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xrefOffset = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        offsets.forEach(offset -> pdf.append(String.format("%010d 00000 n \n", offset)));
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private String pdfText(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", "");
    }

    private void logCredentials() {
        log.info(">>>> [SHOWCASE-SEED] Acceso de muestra -> email: {} | password: {} | clinica: {}",
                email, password, clinicName);
    }

    private record InventoryFixture(
            Material gloves,
            Material lidocaine,
            Material resin,
            Material needles,
            Material masks) {
    }

    private record PatientFixture(Patient primary, Patient pediatric, Patient geriatric) {
    }

    private record TreatmentFixture(Quotation primaryQuote, Quotation pediatricQuote) {
    }

    private static final class MemoryMultipartFile implements MultipartFile {
        private final String filename;
        private final String contentType;
        private final byte[] content;

        private MemoryMultipartFile(String filename, String contentType, byte[] content) {
            this.filename = filename;
            this.contentType = contentType;
            this.content = content.clone();
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), content);
        }
    }
}
