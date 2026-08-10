import type { BloodType, Gender, MaritalStatus } from "@modules/patients/types";

export const genderOptions: Array<{ value: Gender; label: string }> = [
  { value: "MALE", label: "Masculino" },
  { value: "FEMALE", label: "Femenino" },
  { value: "OTHER", label: "Otro" }
];

export const maritalStatusOptions: Array<{ value: MaritalStatus; label: string }> = [
  { value: "SINGLE", label: "Soltero(a)" },
  { value: "MARRIED", label: "Casado(a)" },
  { value: "DIVORCED", label: "Divorciado(a)" },
  { value: "WIDOWED", label: "Viudo(a)" },
  { value: "COHABITATION", label: "Unión libre" }
];

export const bloodTypeOptions: Array<{ value: BloodType; label: string }> = [
  { value: "O_POSITIVE", label: "O+" },
  { value: "O_NEGATIVE", label: "O-" },
  { value: "A_POSITIVE", label: "A+" },
  { value: "A_NEGATIVE", label: "A-" },
  { value: "B_POSITIVE", label: "B+" },
  { value: "B_NEGATIVE", label: "B-" },
  { value: "AB_POSITIVE", label: "AB+" },
  { value: "AB_NEGATIVE", label: "AB-" }
];

const genderLabels: Record<Gender, string> = {
  MALE: "Masculino",
  FEMALE: "Femenino",
  OTHER: "Otro"
};

const maritalStatusLabels: Record<MaritalStatus, string> = {
  SINGLE: "Soltero(a)",
  MARRIED: "Casado(a)",
  DIVORCED: "Divorciado(a)",
  WIDOWED: "Viudo(a)",
  COHABITATION: "Unión libre"
};

const bloodTypeLabels: Record<BloodType, string> = {
  O_POSITIVE: "O+",
  O_NEGATIVE: "O-",
  A_POSITIVE: "A+",
  A_NEGATIVE: "A-",
  B_POSITIVE: "B+",
  B_NEGATIVE: "B-",
  AB_POSITIVE: "AB+",
  AB_NEGATIVE: "AB-"
};

export function genderLabel(value?: Gender) {
  return value ? genderLabels[value] : undefined;
}

export function maritalStatusLabel(value?: MaritalStatus) {
  return value ? maritalStatusLabels[value] : undefined;
}

export function bloodTypeLabel(value?: BloodType) {
  return value ? bloodTypeLabels[value] : undefined;
}
