package com.jclinical.app.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
