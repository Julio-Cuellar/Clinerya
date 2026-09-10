package com.jclinical.app.dev;

import com.jclinical.clinics.domain.model.Clinic;
import com.jclinical.clinics.domain.ports.in.ManageClinicUseCase;
import com.jclinical.inventory.domain.model.Material;
import com.jclinical.inventory.domain.model.MovementType;
import com.jclinical.inventory.domain.ports.in.ManageInventoryMovementUseCase;
import com.jclinical.inventory.domain.ports.in.ManageInventoryMovementUseCase.RegisterMovementCommand;
import com.jclinical.inventory.domain.ports.in.ManageMaterialUseCase;
import com.jclinical.inventory.domain.ports.in.ManageMaterialUseCase.CreateMaterialCommand;
import com.jclinical.patients.domain.model.Gender;
import com.jclinical.patients.domain.model.Patient;
import com.jclinical.patients.domain.ports.in.RegisterPatientUseCase;
import com.jclinical.treatments.domain.model.Quotation;
import com.jclinical.treatments.domain.model.QuotationStatus;
import com.jclinical.treatments.domain.model.TreatmentCatalogItem;
import com.jclinical.treatments.domain.ports.in.ManageQuotationUseCase;
import com.jclinical.treatments.domain.ports.in.ManageTreatmentCatalogUseCase;
import com.jclinical.users.domain.model.User;
import com.jclinical.users.domain.model.UserPreRegistration;
import com.jclinical.users.domain.ports.in.RegisterUserUseCase;
import com.jclinical.users.domain.ports.in.RegisterUserUseCase.RegisterUserCommand;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Crea (una sola vez) un usuario de prueba ya verificado, con clínica activa, inventario de
 * ejemplo, paciente demo, catálogo de tratamientos y cotización aceptada al arrancar la app,
 * para agilizar pruebas manuales de extremo a extremo en desarrollo.
 * Se activa solo con app.dev-seed.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "app.dev-seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
@Order(100)
public class DevDataSeeder implements ApplicationRunner {

    private final RegisterUserUseCase registerUserUseCase;
    private final VerifyUserEmailUseCase verifyUserEmailUseCase;
    private final UserPreRegistrationRepositoryPort preRegistrationRepository;
    private final UserRepositoryPort userRepository;
    private final ManageClinicUseCase clinicUseCase;
    private final ManageMaterialUseCase materialUseCase;
    private final ManageInventoryMovementUseCase movementUseCase;
    private final RegisterPatientUseCase registerPatientUseCase;
    private final ManageTreatmentCatalogUseCase catalogUseCase;
    private final ManageQuotationUseCase quotationUseCase;

    @Value("${app.dev-seed.email}")
    private String email;

    @Value("${app.dev-seed.password}")
    private String password;

    @Value("${app.dev-seed.full-name}")
    private String fullName;

    @Value("${app.dev-seed.clinic-name}")
    private String clinicName;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(email)) {
            log.info(">>>> [DEV-SEED] El usuario '{}' ya existe, se omite la siembra.", email);
            return;
        }

        registerUserUseCase.registerUser(new RegisterUserCommand(email, password, fullName, clinicName));

        UserPreRegistration preRegistration = preRegistrationRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("No se encontró el pre-registro recién creado para " + email));

        verifyUserEmailUseCase.verifyEmail(preRegistration.getVerificationToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario recién verificado " + email));

        log.info(">>>> [DEV-SEED] Usuario de prueba listo -> email: {} | password: {} | clinica: {}",
                email, password, clinicName);

        List<Clinic> clinics = clinicUseCase.getClinicsByOwner(user.getId());
        if (clinics.isEmpty()) {
            log.warn(">>>> [DEV-SEED] El usuario se creó pero no se encontró clínica auto-aprovisionada; se omite la siembra de datos.");
            return;
        }

        UUID clinicId = clinics.get(0).getId();
        
        // 1. Siembra de Inventario
        List<Material> materials = seedInventory(user.getId(), clinicId);
        Material guantes = materials.get(0);
        Material lidocaina = materials.get(1);

        // 2. Siembra de Paciente
        Patient juan = seedPatient(clinicId);

        // 3. Siembra de Catálogo de Tratamientos
        List<TreatmentCatalogItem> catalogItems = seedCatalog(user.getId(), clinicId, guantes, lidocaina);
        TreatmentCatalogItem limpieza = catalogItems.get(0);
        TreatmentCatalogItem endodoncia = catalogItems.get(1);

        // 4. Siembra de Cotización Aceptada
        seedQuotation(clinicId, user.getId(), juan.getId(), limpieza, endodoncia, guantes, lidocaina);
    }

    private List<Material> seedInventory(UUID actingUserId, UUID clinicId) {
        Material guantes = materialUseCase.createMaterial(actingUserId, clinicId, new CreateMaterialCommand(
                "Guantes de latex talla M", "Desechables", "DESC-001", "Medline",
                "Guantes de exploracion no esteriles", "par", "Caja",
                BigDecimal.valueOf(100), BigDecimal.valueOf(0.85),
                BigDecimal.valueOf(200), false, null, false
        ));
        registerMovement(actingUserId, clinicId, guantes.getId(), MovementType.PURCHASE_ENTRY,
                null, BigDecimal.valueOf(5), null, null, "Compra inicial proveedor Medline");

        Material lidocaina = materialUseCase.createMaterial(actingUserId, clinicId, new CreateMaterialCommand(
                "Lidocaina 2% con epinefrina", "Anestesicos", "ANES-010", "Septodont",
                "Cartuchos de anestesia local", "cartucho", null,
                null, BigDecimal.valueOf(6.50),
                BigDecimal.valueOf(20), false, null, true
        ));
        registerMovement(actingUserId, clinicId, lidocaina.getId(), MovementType.PURCHASE_ENTRY,
                BigDecimal.valueOf(50), null, "L2026A", LocalDate.of(2027, 6, 30), "Compra inicial Septodont");
        registerMovement(actingUserId, clinicId, lidocaina.getId(), MovementType.PURCHASE_ENTRY,
                BigDecimal.valueOf(20), null, "L2026B", LocalDate.of(2026, 9, 15), "Segundo lote, caduca antes");
        registerMovement(actingUserId, clinicId, lidocaina.getId(), MovementType.ADJUSTMENT_OUT,
                BigDecimal.valueOf(5), null, null, null, "Merma por cartucho danado en refrigerador");

        Material cepillo = materialUseCase.createMaterial(actingUserId, clinicId, new CreateMaterialCommand(
                "Cepillo dental adulto", "Higiene", "HIG-005", "Oral-B",
                "Cepillo suave para venta en recepcion", "pieza", null,
                null, BigDecimal.valueOf(18.00),
                BigDecimal.valueOf(10), true, BigDecimal.valueOf(45.00), false
        ));
        registerMovement(actingUserId, clinicId, cepillo.getId(), MovementType.PURCHASE_ENTRY,
                BigDecimal.valueOf(30), null, null, null, "Compra inicial para venta en recepcion");
        registerMovement(actingUserId, clinicId, cepillo.getId(), MovementType.SALE_EXIT,
                BigDecimal.valueOf(3), null, null, null, "Venta mostrador a paciente");

        log.info(">>>> [DEV-SEED] Inventario de ejemplo creado: {} (500 pares), {} (65 cartuchos, 2 lotes), {} (27 piezas, venta habilitada).",
                guantes.getName(), lidocaina.getName(), cepillo.getName());

        return List.of(guantes, lidocaina, cepillo);
    }

    private Patient seedPatient(UUID clinicId) {
        Patient patient = registerPatientUseCase.registerPatient(new RegisterPatientUseCase.RegisterPatientCommand(
                clinicId,
                "Juan", "Perez", "Lopez",
                "PELJ900101HDFLPR01",
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                "5555555555",
                "juan.perez@example.com",
                "Ingeniero",
                null,
                "Mexicana",
                null,
                null,
                null
        ));
        log.info(">>>> [DEV-SEED] Paciente de prueba creado -> nombre: {} {} | CURP: {}", 
                patient.getFirstName(), patient.getLastNamePaterno(), patient.getCurp());
        return patient;
    }

    private List<TreatmentCatalogItem> seedCatalog(UUID actingUserId, UUID clinicId, Material guantes, Material lidocaina) {
        TreatmentCatalogItem limpieza = catalogUseCase.createCatalogItem(actingUserId, clinicId, new ManageTreatmentCatalogUseCase.CreateCatalogItemCommand(
                "Limpieza dental profunda",
                "PREVENTIVE",
                "Limpieza general profunda con ultrasonido.",
                BigDecimal.valueOf(400.00),
                30,
                List.of(new ManageTreatmentCatalogUseCase.CatalogMaterialCommand(guantes.getId(), BigDecimal.valueOf(1.0)))
        ));

        TreatmentCatalogItem endodoncia = catalogUseCase.createCatalogItem(actingUserId, clinicId, new ManageTreatmentCatalogUseCase.CreateCatalogItemCommand(
                "Endodoncia molar",
                "RESTORATIVE",
                "Tratamiento de conductos en pieza molar.",
                BigDecimal.valueOf(1500.00),
                60,
                List.of(
                        new ManageTreatmentCatalogUseCase.CatalogMaterialCommand(guantes.getId(), BigDecimal.valueOf(2.0)),
                        new ManageTreatmentCatalogUseCase.CatalogMaterialCommand(lidocaina.getId(), BigDecimal.valueOf(2.0))
                )
        ));

        log.info(">>>> [DEV-SEED] Catálogo de tratamientos creado: {} (400.00 MXN), {} (1,500.00 MXN).",
                limpieza.getName(), endodoncia.getName());

        return List.of(limpieza, endodoncia);
    }

    private void seedQuotation(
            UUID clinicId,
            UUID userId,
            UUID patientId,
            TreatmentCatalogItem limpieza,
            TreatmentCatalogItem endodoncia,
            Material guantes,
            Material lidocaina) {

        ManageQuotationUseCase.MaterialLineCommand qMat1 = new ManageQuotationUseCase.MaterialLineCommand(
                guantes.getId(),
                guantes.getName(),
                BigDecimal.valueOf(1.0),
                guantes.getUnitCost()
        );
        ManageQuotationUseCase.QuotationItemCommand qItem1 = new ManageQuotationUseCase.QuotationItemCommand(
                limpieza.getId(),
                limpieza.getName(),
                null,
                limpieza.getDefaultPrice(),
                List.of(qMat1),
                BigDecimal.ZERO
        );

        ManageQuotationUseCase.MaterialLineCommand qMat2_1 = new ManageQuotationUseCase.MaterialLineCommand(
                guantes.getId(),
                guantes.getName(),
                BigDecimal.valueOf(2.0),
                guantes.getUnitCost()
        );
        ManageQuotationUseCase.MaterialLineCommand qMat2_2 = new ManageQuotationUseCase.MaterialLineCommand(
                lidocaina.getId(),
                lidocaina.getName(),
                BigDecimal.valueOf(2.0),
                lidocaina.getUnitCost()
        );
        ManageQuotationUseCase.QuotationItemCommand qItem2 = new ManageQuotationUseCase.QuotationItemCommand(
                endodoncia.getId(),
                endodoncia.getName(),
                46,
                endodoncia.getDefaultPrice(),
                List.of(qMat2_1, qMat2_2),
                BigDecimal.ZERO
        );

        Quotation quotation = quotationUseCase.createQuotation(
                userId,
                patientId,
                clinicId,
                new ManageQuotationUseCase.CreateQuotationCommand(
                        userId,
                        LocalDate.now(),
                        "Presupuesto inicial para tratamiento integral de endodoncia y limpieza.",
                        LocalDate.now().plusDays(30),
                        List.of(qItem1, qItem2)
                )
        );

        // Aceptar cotización automáticamente para habilitar el registro de visitas y citas
        quotationUseCase.transitionStatus(userId, quotation.getId(), patientId, clinicId, QuotationStatus.SENT);
        quotationUseCase.transitionStatus(userId, quotation.getId(), patientId, clinicId, QuotationStatus.ACCEPTED);

        log.info(">>>> [DEV-SEED] Cotización aceptada creada de prueba -> ID: {} | Total: {} MXN",
                quotation.getId(), quotation.grandTotal());
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
        RegisterMovementCommand command = new RegisterMovementCommand(
                type, quantity, presentationQuantity, null, notes, lotNumber, expirationDate, null
        );
        switch (type) {
            case PURCHASE_ENTRY -> movementUseCase.registerPurchaseEntry(actingUserId, clinicId, materialId, command);
            case ADJUSTMENT_IN, ADJUSTMENT_OUT -> movementUseCase.registerAdjustment(actingUserId, clinicId, materialId, command);
            case SALE_EXIT -> movementUseCase.registerSaleExit(actingUserId, clinicId, materialId, command);
            case USAGE_EXIT -> throw new IllegalStateException("USAGE_EXIT no se siembra automáticamente.");
        }
    }
}
