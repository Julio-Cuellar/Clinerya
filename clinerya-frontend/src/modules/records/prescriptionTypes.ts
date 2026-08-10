export interface PrescriptionItem {
  id?: string;
  medicationName: string;
  dosage: string;
  frequency: string;
  duration: string;
  instructions?: string;
}

export interface Prescription {
  id: string;
  clinicId: string;
  patientId: string;
  doctorId?: string;
  appointmentId?: string;
  notes?: string;
  status: string;
  items: PrescriptionItem[];
  createdAt: string;
  updatedAt: string;
}

export interface IssuePrescriptionRequest {
  patientId: string;
  doctorId?: string;
  appointmentId?: string;
  notes?: string;
  items: Omit<PrescriptionItem, "id">[];
}
