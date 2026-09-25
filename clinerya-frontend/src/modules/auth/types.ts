export type ThemePreference = "LIGHT" | "DARK" | "SYSTEM";

export interface UserClinicSummary {
  id: string;
  name: string;
  role: string;
  permissions?: string[];
  /** Atiende pacientes en esta clínica (DOCTOR, o administrador que además atiende). */
  attendsPatients?: boolean;
}

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  phone?: string;
  avatarUrl?: string;
  emailVerified: boolean;
  themePreference: ThemePreference;
  active: boolean;
  clinics: UserClinicSummary[];
}

export interface AuthUserProfile {
  id: string;
  email: string;
  fullName: string;
  phone?: string;
  avatarUrl?: string;
  emailVerified: boolean;
  themePreference: ThemePreference;
  active: boolean;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  clinicName: string;
}

export interface VerifyEmailRequest {
  token: string;
}

export interface MessageResponse {
  message: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  user: AuthUserProfile;
}

export interface RefreshResponse {
  token: string;
  tokenType: string;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}
