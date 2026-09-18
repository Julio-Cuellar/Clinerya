package com.jclinical.treatments.domain.model;

import com.jclinical.core.domain.ClinicSpecialty;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Catálogos de arranque sugeridos por especialidad.
 *
 * <p><b>Los precios son de referencia, no una tarifa.</b> Existen para que una clínica nueva pueda
 * cotizar el mismo día en lugar de empezar con una pantalla vacía; se espera que los ajuste a su
 * plaza y a su costo real antes de usarlos con pacientes.
 *
 * <p>Ningún procedimiento trae materiales: una clínica recién dada de alta no tiene inventario al
 * que engancharlos. El médico añade los insumos típicos cuando ya dio de alta sus materiales.
 */
public final class TreatmentCatalogSeeds {

    private TreatmentCatalogSeeds() {
    }

    /** Definición de un servicio sembrado. El nombre es la clave natural dentro de la clínica. */
    public record SeedItem(String name, String category, BigDecimal defaultPrice, Integer estimatedDurationMinutes) {
    }

    private static SeedItem item(String name, String category, long price, Integer minutes) {
        return new SeedItem(name, category, BigDecimal.valueOf(price), minutes);
    }

    private static final List<SeedItem> ODONTOLOGIA = List.of(
            item("Consulta y diagnóstico", "Diagnóstico", 400, 30),
            item("Radiografía periapical", "Diagnóstico", 250, 15),
            item("Limpieza dental (profilaxis)", "Preventiva", 800, 45),
            item("Aplicación de flúor", "Preventiva", 450, 20),
            item("Sellador de fosetas y fisuras", "Preventiva", 600, 30),
            item("Resina simple (una superficie)", "Restaurativa", 950, 45),
            item("Resina compuesta (dos o más superficies)", "Restaurativa", 1300, 60),
            item("Incrustación", "Restaurativa", 3900, 90),
            item("Endodoncia unirradicular", "Endodoncia", 2800, 90),
            item("Endodoncia multirradicular", "Endodoncia", 4200, 120),
            item("Extracción simple", "Cirugía", 900, 40),
            item("Extracción quirúrgica (tercer molar)", "Cirugía", 2400, 90),
            item("Profilaxis periodontal", "Periodoncia", 1600, 60),
            item("Curetaje por cuadrante", "Periodoncia", 1800, 60),
            item("Corona de porcelana", "Prótesis", 6500, 120),
            item("Prótesis total (por arcada)", "Prótesis", 9800, 150),
            item("Placa oclusal (guarda nocturna)", "Prótesis", 3200, 60),
            item("Blanqueamiento dental", "Estética", 3500, 90));

    private static final List<SeedItem> MEDICINA_GENERAL = List.of(
            item("Consulta de primera vez", "Consulta", 800, 45),
            item("Consulta de seguimiento", "Consulta", 500, 20),
            item("Certificado médico", "Consulta", 400, 20),
            item("Toma de signos vitales", "Procedimiento", 150, 10),
            item("Aplicación de inyección", "Procedimiento", 150, 10),
            item("Curación de herida", "Procedimiento", 450, 30),
            item("Retiro de puntos", "Procedimiento", 350, 20),
            item("Nebulización", "Procedimiento", 400, 30),
            item("Consulta a domicilio", "Consulta", 1200, 60));

    private static final List<SeedItem> NUTRICION = List.of(
            item("Consulta de primera vez", "Consulta", 900, 60),
            item("Consulta de seguimiento", "Consulta", 600, 30),
            item("Plan alimenticio personalizado", "Plan", 1200, 45),
            item("Medición de composición corporal", "Evaluación", 500, 30),
            item("Plan de nutrición deportiva", "Plan", 1400, 45),
            item("Asesoría grupal", "Consulta", 400, 60),
            item("Paquete mensual de seguimiento", "Paquete", 2200, null));

    private static final List<SeedItem> FISIOTERAPIA = List.of(
            item("Valoración inicial", "Evaluación", 800, 60),
            item("Sesión de terapia física", "Sesión", 600, 45),
            item("Paquete de 10 sesiones", "Paquete", 5200, null),
            item("Electroterapia", "Sesión", 450, 30),
            item("Ultrasonido terapéutico", "Sesión", 450, 30),
            item("Terapia manual", "Sesión", 700, 45),
            item("Rehabilitación posquirúrgica", "Sesión", 900, 60),
            item("Vendaje neuromuscular", "Procedimiento", 350, 20),
            item("Terapia respiratoria", "Sesión", 700, 45),
            item("Masaje terapéutico", "Sesión", 600, 45),
            item("Reeducación postural", "Sesión", 750, 50));

    private static final List<SeedItem> OFTALMOLOGIA = List.of(
            item("Consulta oftalmológica", "Consulta", 800, 30),
            item("Examen de agudeza visual", "Estudio", 400, 20),
            item("Refracción y graduación", "Estudio", 600, 30),
            item("Topografía corneal", "Estudio", 1200, 30),
            item("Campimetría", "Estudio", 1400, 40),
            item("Tonometría", "Estudio", 400, 15),
            item("Revisión de fondo de ojo", "Estudio", 700, 30),
            item("Retiro de cuerpo extraño", "Procedimiento", 900, 30),
            item("Adaptación de lentes de contacto", "Procedimiento", 1100, 45),
            item("Control posoperatorio", "Consulta", 500, 20));

    private static final Map<ClinicSpecialty, List<SeedItem>> BY_SPECIALTY = Map.of(
            ClinicSpecialty.ODONTOLOGIA, ODONTOLOGIA,
            ClinicSpecialty.MEDICINA_GENERAL, MEDICINA_GENERAL,
            ClinicSpecialty.NUTRICION, NUTRICION,
            ClinicSpecialty.FISIOTERAPIA, FISIOTERAPIA,
            ClinicSpecialty.OFTALMOLOGIA, OFTALMOLOGIA);

    /**
     * Servicios sugeridos para una especialidad. Vacío para {@code OTRA} y {@code SIN_CONFIGURAR}:
     * no hay un catálogo genérico que valga la pena imponer, esas clínicas arrancan en blanco.
     */
    public static List<SeedItem> forSpecialty(ClinicSpecialty specialty) {
        if (specialty == null) {
            return List.of();
        }
        return BY_SPECIALTY.getOrDefault(specialty, List.of());
    }
}
