export type AppointmentStatus = "SCHEDULED" | "CONFIRMED" | "COMPLETED" | "CANCELLED" | "NO_SHOW";

export const APPOINTMENT_STATUS_LABELS: Record<AppointmentStatus, string> = {
  SCHEDULED: "Programada",
  CONFIRMED: "Confirmada",
  COMPLETED: "Completada",
  CANCELLED: "Cancelada",
  NO_SHOW: "No asistió"
};

export interface ClinicRoomResponse {
  id: string;
  clinicId: string;
  name: string;
  code?: string;
  colorHex?: string;
  description?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export type RoomBlockType = "MAINTENANCE" | "CLEANING" | "INTERNAL_USE" | "UNAVAILABLE" | "OTHER";

export interface RoomBlockResponse {
  id: string;
  clinicId: string;
  roomId: string;
  startsAt: string;
  endsAt: string;
  type: RoomBlockType;
  reason?: string;
  createdByUserId?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AppointmentResponse {
  id: string;
  clinicId: string;
  patientId?: string;
  doctorStaffId: string;
  roomId?: string;
  quotationId?: string;
  quotationItemId?: string;
  quotationItemIds?: string[];
  seriesId?: string;
  scheduledStart: string;
  scheduledEnd: string;
  reason?: string;
  notes?: string;
  cancellationReason?: string;
  cancelledAt?: string;
  cancelledByUserId?: string;
  status: AppointmentStatus;
  materialsReserved?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAppointmentRequest {
  patientId?: string;
  doctorStaffId: string;
  roomId?: string;
  quotationId?: string;
  quotationItemId?: string;
  quotationItemIds?: string[];
  scheduledStart: string;
  scheduledEnd: string;
  reason?: string;
  notes?: string;
}

export type RecurrenceFrequency = "DAILY" | "WEEKLY" | "BIWEEKLY" | "MONTHLY";

export interface CreateAppointmentSeriesRequest {
  patientId?: string;
  doctorStaffId: string;
  roomId?: string;
  quotationId?: string;
  quotationItemId?: string;
  quotationItemIds?: string[];
  firstScheduledStart: string;
  firstScheduledEnd: string;
  frequency: RecurrenceFrequency;
  repeatCount: number;
  reason?: string;
  notes?: string;
}

export interface RescheduleAppointmentRequest {
  scheduledStart: string;
  scheduledEnd: string;
}

export interface DoctorResponse {
  staffId: string;
  fullName: string;
}

export type WaitingListStatus = "WAITING" | "NOTIFIED" | "SCHEDULED" | "CANCELLED";

export interface WaitingListEntryResponse {
  id: string;
  clinicId: string;
  patientId: string;
  doctorStaffId?: string;
  roomId?: string;
  preferredDateFrom?: string;
  preferredDateTo?: string;
  preferredTimeRange?: string;
  notes?: string;
  status: WaitingListStatus;
  createdAt: string;
  updatedAt: string;
}

export interface AddToWaitingListRequest {
  patientId: string;
  doctorStaffId?: string;
  roomId?: string;
  preferredDateFrom?: string;
  preferredDateTo?: string;
  preferredTimeRange?: string;
  notes?: string;
}

export type WeekDay = "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY";

export interface DayScheduleResponse {
  dayOfWeek: WeekDay;
  open: boolean;
  startTime: string | null;
  endTime: string | null;
}

export interface DayScheduleRequest {
  dayOfWeek: WeekDay;
  open: boolean;
  startTime: string | null;
  endTime: string | null;
}
