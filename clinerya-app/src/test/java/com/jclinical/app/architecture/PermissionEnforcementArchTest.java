package com.jclinical.app.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Exige que todo controlador REST montado bajo {@code /api/v1/clinics/**} o
 * {@code /api/v1/patients/**} resuelva al usuario autenticado que hace la
 * llamada, ya sea inyectando {@code CurrentUserResolver} o recibiendo un
 * {@link java.security.Principal} en alguno de sus métodos. Es la barrera que
 * garantiza que la autorización por permisos (aplicada en la capa de dominio con
 * {@code StaffPermissionCheckerPort}) reciba siempre un {@code actingUserId} real.
 *
 * <p>La resolución de acceso vive en el dominio; este test solo verifica que el
 * adaptador web sepa <em>quién</em> llama.
 */
class PermissionEnforcementArchTest {

    private static final String CURRENT_USER_RESOLVER =
            "com.jclinical.users.infra.security.CurrentUserResolver";

    private static final List<String> GUARDED_PATH_PREFIXES = List.of(
            "/api/v1/clinics/",
            "/api/v1/patients/");

    /**
     * Controladores que ya existían cuando se introdujo esta regla y todavía no
     * resuelven al usuario autenticado en el adaptador web. Es deuda conocida y
     * acotada: <strong>solo se quita de esta lista</strong> (nunca se agrega). Al
     * migrar un controlador —inyectando {@code CurrentUserResolver} o recibiendo
     * un {@link java.security.Principal}, y aplicando la autorización por permiso
     * en su servicio de dominio— se borra su entrada de aquí. Un controlador
     * nuevo que aparezca en la lista de violaciones es un fallo real, no una
     * excepción.
     *
     * <p>Vacía: todos los controladores bajo rutas protegidas resuelven al
     * usuario autenticado.
     */
    private static final Set<String> PRE_EXISTING_EXCEPTIONS = Set.of();

    private final JavaClasses controllers = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.jclinical");

    @Test
    void everyClinicOrPatientScopedControllerResolvesTheAuthenticatedUser() {
        List<String> violations = new ArrayList<>();

        for (JavaClass controller : controllers) {
            if (!controller.isAnnotatedWith(RestController.class)) {
                continue;
            }
            if (!isGuardedByPath(controller)) {
                continue;
            }
            if (PRE_EXISTING_EXCEPTIONS.contains(controller.getName())
                    || PRE_EXISTING_EXCEPTIONS.contains(controller.getSimpleName())) {
                continue;
            }
            if (!resolvesAuthenticatedUser(controller)) {
                violations.add(controller.getName()
                        + " expone endpoints bajo una ruta protegida pero no inyecta "
                        + "CurrentUserResolver ni recibe java.security.Principal en ningún método.");
            }
        }

        if (!violations.isEmpty()) {
            fail("Controladores sin resolución del usuario autenticado:\n  - "
                    + String.join("\n  - ", violations));
        }
    }

    @Test
    void preExistingExceptionsListHasNoStaleEntries() {
        List<String> stale = new ArrayList<>();

        for (String excepted : PRE_EXISTING_EXCEPTIONS) {
            JavaClass controller = controllers.stream()
                    .filter(candidate -> candidate.getName().equals(excepted))
                    .findFirst()
                    .orElse(null);
            if (controller == null) {
                stale.add(excepted + " ya no existe en el classpath; bórralo de la lista.");
            } else if (!isGuardedByPath(controller)) {
                stale.add(excepted + " ya no expone rutas protegidas; bórralo de la lista.");
            } else if (resolvesAuthenticatedUser(controller)) {
                stale.add(excepted + " ya resuelve al usuario autenticado; bórralo de la lista.");
            }
        }

        if (!stale.isEmpty()) {
            fail("PRE_EXISTING_EXCEPTIONS contiene entradas obsoletas:\n  - "
                    + String.join("\n  - ", stale));
        }
    }

    private static boolean isGuardedByPath(JavaClass controller) {
        return mappedPaths(controller).stream()
                .anyMatch(path -> GUARDED_PATH_PREFIXES.stream().anyMatch(path::startsWith));
    }

    private static List<String> mappedPaths(JavaClass controller) {
        List<String> paths = new ArrayList<>();
        controller.tryGetAnnotationOfType(RequestMapping.class)
                .ifPresent(mapping -> {
                    for (String value : mapping.path()) {
                        paths.add(normalize(value));
                    }
                    for (String value : mapping.value()) {
                        paths.add(normalize(value));
                    }
                });
        return paths;
    }

    private static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static boolean resolvesAuthenticatedUser(JavaClass controller) {
        boolean injectsResolver = controller.getAllFields().stream()
                .anyMatch(field -> field.getRawType().getName().equals(CURRENT_USER_RESOLVER))
                || controller.getConstructors().stream()
                        .flatMap(constructor -> constructor.getRawParameterTypes().stream())
                        .anyMatch(type -> type.getName().equals(CURRENT_USER_RESOLVER));
        if (injectsResolver) {
            return true;
        }
        for (JavaMethod method : controller.getMethods()) {
            for (JavaClass parameter : method.getRawParameterTypes()) {
                if (parameter.isAssignableTo(Principal.class)) {
                    return true;
                }
            }
        }
        return false;
    }
}
