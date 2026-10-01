package com.jclinical.automation.domain.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ninguna cifra, enlace o correo de la respuesta puede salir de la nada: todo tiene que estar en lo
 * que el agente consulto o en lo que ya se dijo en el chat. Los digitos sueltos ("te muestro 3
 * opciones") no cuentan como dato.
 */
class GroundingGuardTest {

    private final List<String> sources = List.of("Limpieza dental $650", "Resina $1,200.50", "Jue 01/10 16:00",
            "Teléfono 222 555 0101", "Hoy es domingo 27 de septiembre de 2026 y son las 21:00");

    @Test
    void figuresThatComeFromTheSourcesPass() {
        assertTrue(GroundingGuard.inventedFigures("La limpieza está desde $650 y te puedo dar el Jue 01/10 16:00.", sources).isEmpty());
        assertTrue(GroundingGuard.inventedFigures("Márcanos al 222 555 0101.", sources).isEmpty());
    }

    @Test
    void theSameFigureWrittenAnotherWayStillPasses() {
        assertTrue(GroundingGuard.inventedFigures("La resina cuesta 1200.50 pesos, y la limpieza 650.", sources).isEmpty());
    }

    @Test
    void timesAndDatesWrittenWithoutLeadingZerosAreTheSameFigure() {
        List<String> scheduleSources = List.of("{horarios=[Jue 01/10 08:00, Jue 01/10 08:30, Jue 01/10 16:00]}");

        assertTrue(GroundingGuard.inventedFigures("Tengo a las 8:00 y 8:30 el 1/10.", scheduleSources).isEmpty());
        assertTrue(GroundingGuard.inventedFigures("El Jue 01/10 a las 4:00.", List.of("Jue 1/10 04:00")).isEmpty());
        assertEquals(List.of("8:15"), GroundingGuard.inventedFigures("Tengo a las 8:15.", scheduleSources),
                "otra hora sigue siendo inventada");
        assertEquals(List.of("2/10"), GroundingGuard.inventedFigures("El 2/10 tengo lugar.", scheduleSources));
    }

    @Test
    void singleDigitsAreConversationNotData() {
        assertTrue(GroundingGuard.inventedFigures("Te comparto 3 opciones; elige 1.", sources).isEmpty());
    }

    @Test
    void whatThePatientSaidCanBeRepeatedExceptAPrice() {
        List<String> patientSaid = List.of("¿tienen a las 17:15? me dijeron que cuesta $100");

        assertTrue(GroundingGuard.inventedFigures("A las 17:15 no tengo, pero sí el Jue 01/10 16:00.", sources, patientSaid).isEmpty());
        assertEquals(List.of("100"), GroundingGuard.inventedFigures("Sí, la limpieza cuesta $100.", sources, patientSaid),
                "un precio solo puede venir de la clinica, nunca de lo que dijo el paciente");
    }

    @Test
    void anInventedPriceTimePhoneLinkOrEmailIsCaught() {
        assertEquals(List.of("700"), GroundingGuard.inventedFigures("La limpieza cuesta $700.", sources));
        assertEquals(List.of("17:15"), GroundingGuard.inventedFigures("Tengo a las 17:15.", sources));
        assertEquals(List.of("5555555555"), GroundingGuard.inventedFigures("Llama al 5555555555.", sources));
        assertEquals(List.of("https://promo.example.com"), GroundingGuard.inventedFigures("Mira https://promo.example.com", sources));
        assertEquals(List.of("citas@clinica.mx"), GroundingGuard.inventedFigures("Escribe a citas@clinica.mx", sources));
    }
}
