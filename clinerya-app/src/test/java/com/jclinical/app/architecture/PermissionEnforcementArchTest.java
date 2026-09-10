package com.jclinical.app.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * P3: la interceptor de acceso a clinica solo comprobaba "eres staff activo de esta
 * clinica", nunca el permiso especifico de la operacion. La autorizacion real vive en el
 * dominio (StaffPermissionCheckerPort / PatientAccessAuthorizationPort), y ambos exigen
 * conocer al usuario que hace la peticion. Esta prueba falla si un controlador nuevo bajo
 * /api/v1/clinics/** o /api/v1/patients/** no tiene ninguna forma de resolver ese usuario
 * (ni CurrentUserResolver ni java.security.Principal), porque sin eso es estructuralmente
 * imposible que el controlador aplique el chequeo de permisos.
 *
 * No es una prueba de que el permiso correcto se aplique — eso lo cubren los tests de
 * autorizacion de cada servicio — sino una red minima contra "se me olvido conectar el
 * chequeo por completo", que es exactamente como se veian los hallazgos de este plan.
 */
@AnalyzeClasses(packages = "com.jclinical", importOptions = ImportOption.DoNotIncludeTests.class)
class PermissionEnforcementArchTest {

    private static final String CURRENT_USER_RESOLVER = "com.jclinical.users.infra.security.CurrentUserResolver";

    /**
     * Excepciones preexistentes, fuera del alcance de esta fase (P3 solo cubrio caja,
     * contabilidad, tratamientos, inventario, agenda y clinicas). No es una lista abierta:
     * un controlador nuevo no puede sumarse aqui, debe resolver al usuario autenticado.
     */
    private static final Set<String> PRE_EXISTING_EXCEPTIONS = Set.of(
            "com.jclinical.staff.infra.adapters.in.web.StaffCompensationController",
            "com.jclinical.staff.infra.adapters.in.web.StaffOnboardingController",
            "com.jclinical.staff.infra.adapters.in.web.StaffOperationsController",
            "com.jclinical.integrations.infra.adapters.in.web.ExternalCalendarEventController",
            "com.jclinical.integrations.infra.adapters.in.web.GoogleCalendarController",
            "com.jclinical.notifications.infra.adapters.in.web.NotificationController"
    );

    private static final DescribedPredicate<JavaClass> CLINIC_OR_PATIENT_CONTROLLERS = DescribedPredicate.describe(
            "sean controladores REST mapeados bajo /api/v1/clinics o /api/v1/patients "
                    + "(sin contar excepciones preexistentes documentadas)",
            javaClass -> javaClass.isAnnotatedWith(RestController.class)
                    && mapsUnderClinicsOrPatients(javaClass)
                    && !PRE_EXISTING_EXCEPTIONS.contains(javaClass.getFullName()));

    @ArchTest
    static final ArchRule clinicAndPatientControllersMustResolveTheActingUser = classes()
            .that(CLINIC_OR_PATIENT_CONTROLLERS)
            .should(new ArchCondition<JavaClass>(
                    "resolver al usuario autenticado (campo CurrentUserResolver o parametro java.security.Principal)") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    boolean resolvesActingUser = hasCurrentUserResolverField(javaClass) || hasPrincipalParameter(javaClass);
                    if (!resolvesActingUser) {
                        events.add(SimpleConditionEvent.violated(javaClass,
                                javaClass.getFullName() + " esta bajo /api/v1/clinics o /api/v1/patients pero no "
                                        + "tiene forma de conocer al usuario autenticado (ni CurrentUserResolver ni "
                                        + "Principal), asi que no puede aplicar el chequeo de permisos del dominio. "
                                        + "Inyecta CurrentUserResolver y pasa el actingUserId al caso de uso."));
                    }
                }
            });

    private static boolean mapsUnderClinicsOrPatients(JavaClass javaClass) {
        if (!javaClass.isAnnotatedWith(RequestMapping.class)) {
            return false;
        }
        RequestMapping mapping = javaClass.getAnnotationOfType(RequestMapping.class);
        for (String path : mapping.value()) {
            if (path.startsWith("/api/v1/clinics") || path.startsWith("/api/v1/patients")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCurrentUserResolverField(JavaClass javaClass) {
        return javaClass.getFields().stream()
                .anyMatch(field -> field.getRawType().getFullName().equals(CURRENT_USER_RESOLVER));
    }

    private static boolean hasPrincipalParameter(JavaClass javaClass) {
        return javaClass.getMethods().stream()
                .flatMap(method -> method.getRawParameterTypes().stream())
                .anyMatch(paramType -> paramType.isAssignableTo(Principal.class));
    }
}
