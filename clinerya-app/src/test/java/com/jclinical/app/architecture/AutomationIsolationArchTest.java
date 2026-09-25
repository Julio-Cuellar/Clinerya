package com.jclinical.app.architecture;

import com.jclinical.automation.archfixture.AutomationReadingAgendaTables;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * La automatizacion escucha al resto del sistema (eventos y puertos de lectura), pero nadie depende
 * de ella: si se apaga o falla, agenda, pacientes y caja siguen igual. Y su dominio no conoce a
 * ningun otro modulo, solo el nucleo compartido.
 */
class AutomationIsolationArchTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.jclinical");

    @Test
    void noOtherModuleDependsOnAutomation() {
        noClasses().that().resideOutsideOfPackage("com.jclinical.automation..")
                .should().dependOnClassesThat().resideInAPackage("com.jclinical.automation..")
                .because("los demas modulos solo publican eventos; la automatizacion es un consumidor opcional")
                .check(CLASSES);
    }

    /**
     * Decision 19: la automatizacion nunca lee los datos de otro modulo por su persistencia
     * (entidades, repositorios, adaptadores de salida); solo por su API publica.
     */
    static final ArchRule AUTOMATION_USES_ONLY_PUBLIC_APIS = noClasses()
            .that().resideInAPackage("com.jclinical.automation..")
            .should().dependOnClassesThat(anotherModulesPersistence())
            .because("el agente jamas consulta la base de otro modulo; solo su API publica");

    private static DescribedPredicate<JavaClass> anotherModulesPersistence() {
        return JavaClass.Predicates.resideInAPackage("..persistence..")
                .or(CanBeAnnotated.Predicates.annotatedWith(Entity.class))
                .or(JavaClass.Predicates.assignableTo(Repository.class))
                .and(JavaClass.Predicates.resideOutsideOfPackage("com.jclinical.automation.."))
                .as("la persistencia de otro modulo (entidades, repositorios o adaptadores de datos)");
    }

    @Test
    void theAutomationNeverReachesIntoAnotherModulesPersistence() {
        AUTOMATION_USES_ONLY_PUBLIC_APIS.check(CLASSES);
    }

    @Test
    void theDataAccessRuleCatchesAnAutomationClassReadingAgendaTables() {
        JavaClasses violation = new ClassFileImporter().importClasses(AutomationReadingAgendaTables.class);

        assertThrows(AssertionError.class, () -> AUTOMATION_USES_ONLY_PUBLIC_APIS.check(violation));
    }

    @Test
    void theAutomationDomainOnlyKnowsTheSharedCore() {
        noClasses().that().resideInAPackage("com.jclinical.automation.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.jclinical.agenda..", "com.jclinical.patients..", "com.jclinical.staff..",
                        "com.jclinical.clinics..", "com.jclinical.users..", "org.springframework..")
                .because("el dominio de la automatizacion habla con el resto solo por sus puertos")
                .check(CLASSES);
    }
}
