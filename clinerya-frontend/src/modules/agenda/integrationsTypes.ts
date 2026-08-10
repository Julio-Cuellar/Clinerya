export interface CalendarConnectionStatusResponse {
  connected: boolean;
  email?: string;
}

export interface AuthorizationUrlResponse {
  authorizationUrl: string;
}

export interface ExternalCalendarEventResponse {
  id: string;
  clinicId: string;
  staffId: string;
  googleEventId: string;
  summary: string | null;
  description: string | null;
  startTime: string;
  endTime: string;
  status: "PENDING_REVIEW" | "LINKED" | "DISMISSED";
  linkedAppointmentId: string | null;
  createdAt: string;
  updatedAt: string;
}

