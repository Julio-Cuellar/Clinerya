import { buildNomStarterPages } from "@modules/records/constants/nomHistoryTemplate";
import { buildNomOdontologyStarterPages } from "@modules/records/constants/nomOdontologyTemplate";
import { serializeSchema } from "@modules/records/types";
import type { CreateHistoryTemplateRequest } from "@modules/records/types";

/**
 * Definición de la plantilla de historia clínica recomendada por perfil de clínica.
 *
 * Vive aquí y no dentro de `TemplatesPanel` porque ahora la usan dos lugares: el panel de
 * plantillas y el asistente de alta de la clínica. Duplicar el nombre y la descripción llevaría a
 * que una clínica sembrada desde el onboarding tuviera una plantilla distinta de la sembrada
 * desde el expediente.
 */
export function buildRecommendedTemplate(
  kind: "NOM_013" | "NOM_004"
): CreateHistoryTemplateRequest {
  if (kind === "NOM_013") {
    return {
      name: "Historia clínica odontológica (NOM-013)",
      description:
        "Plantilla base para consultorios dentales basada en el formato del cliente. Incluye antecedentes, interrogatorio por sistemas, odontograma, indice CPOD, consentimiento de tratamiento, contrato de servicios y consentimiento de uso de la app.",
      schemaJson: serializeSchema({ kind: "historia_clinica", pages: buildNomOdontologyStarterPages() })
    };
  }
  return {
    name: "Historia clínica general (NOM-004)",
    description: "Plantilla base sugerida por la NOM-004-SSA3-2012, editable a criterio de la clínica.",
    schemaJson: serializeSchema({ kind: "historia_clinica", pages: buildNomStarterPages() })
  };
}
