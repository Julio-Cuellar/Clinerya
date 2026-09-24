import {
  IconActivity,
  IconDental,
  IconDots,
  IconEye,
  IconSalad,
  IconStethoscope
} from "@tabler/icons-react";
import type { TablerIcon } from "@tabler/icons-react";
import type { ClinicSpecialty } from "@modules/clinics/types";
import { SELECTABLE_CLINIC_SPECIALTIES, getClinicProfile } from "@shared/utils/clinicProfile";

const SPECIALTY_ICONS: Record<ClinicSpecialty, TablerIcon> = {
  SIN_CONFIGURAR: IconDots,
  ODONTOLOGIA: IconDental,
  MEDICINA_GENERAL: IconStethoscope,
  NUTRICION: IconSalad,
  FISIOTERAPIA: IconActivity,
  OFTALMOLOGIA: IconEye,
  OTRA: IconDots
};

/**
 * Rejilla de especialidades más el resumen de lo que cambia al elegir una. Controlado, sin guardar
 * nada: lo usan el panel de configuración y el asistente de alta, que persisten de forma distinta.
 */
export function ClinicSpecialtyPicker({
  selected,
  onSelect
}: {
  selected: ClinicSpecialty;
  onSelect: (specialty: ClinicSpecialty) => void;
}) {
  const profile = getClinicProfile(selected);

  return (
    <>
      <div className="clinic-specialty-grid">
        {SELECTABLE_CLINIC_SPECIALTIES.map((specialty) => {
          const option = getClinicProfile(specialty);
          const Icon = SPECIALTY_ICONS[specialty];
          const active = selected === specialty;
          return (
            <button
              key={specialty}
              type="button"
              className={`clinic-specialty-option${active ? " active" : ""}`}
              aria-pressed={active}
              onClick={() => onSelect(specialty)}
            >
              <Icon size={20} aria-hidden="true" />
              <strong>{option.label}</strong>
              <span>{option.recommendedHistoryTemplate === "NOM_013" ? "NOM-013" : "NOM-004"}</span>
            </button>
          );
        })}
      </div>

      <ul className="clinic-specialty-summary">
        <li>
          Las partidas de cotizacion dicen <strong>{profile.laborLabel.toLowerCase()}</strong>.
        </li>
        <li>
          {profile.showsClinicalLocator
            ? `Cada partida puede registrar ${profile.locatorColumnLabel.toLowerCase()} en notacion FDI.`
            : "Las partidas no registran localizador clinico; la zona va en la descripcion."}
        </li>
        <li>
          Se recomienda la plantilla{" "}
          <strong>{profile.recommendedHistoryTemplate === "NOM_013" ? "NOM-013 (Odontologia)" : "NOM-004"}</strong>.
        </li>
        {profile.hasSuggestedCatalog && <li>Hay un catalogo de servicios de arranque que puedes sembrar.</li>}
      </ul>
    </>
  );
}
