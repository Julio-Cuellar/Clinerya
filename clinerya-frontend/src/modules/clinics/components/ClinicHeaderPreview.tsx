import { IconBuildingHospital } from "@tabler/icons-react";
import type { ClinicResponse } from "@modules/clinics/types";

export function formatClinicAddress(clinic: ClinicResponse): string {
  return [clinic.addressStreet, clinic.addressColonia, clinic.addressMunicipality, clinic.addressState, clinic.addressZip]
    .filter(Boolean)
    .join(", ");
}

export function ClinicHeaderPreview({ clinic, loading }: { clinic: ClinicResponse | null; loading: boolean }) {
  if (loading) return <div className="clinic-header-preview">Cargando datos de la clínica...</div>;
  if (!clinic) return <div className="clinic-header-preview">Sin datos de clínica disponibles.</div>;

  const address = formatClinicAddress(clinic);

  return (
    <div className="clinic-header-preview">
      {clinic.logoUrl ? (
        <img src={clinic.logoUrl} alt="Logotipo de la clínica" className="clinic-header-logo" />
      ) : (
        <div className="clinic-header-logo-placeholder">
          <IconBuildingHospital size={22} aria-hidden="true" />
        </div>
      )}
      <div className="clinic-header-text">
        <strong>{clinic.legalName || clinic.name}</strong>
        {address && <span>{address}</span>}
        {clinic.phone && <span>Tel. {clinic.phone}</span>}
        {clinic.email && <span>{clinic.email}</span>}
      </div>
    </div>
  );
}
