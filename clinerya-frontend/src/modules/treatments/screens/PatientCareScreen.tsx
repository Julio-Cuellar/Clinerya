import { FormEvent, useEffect, useState } from "react";
import { priceLabel } from "../logic/catalogRules";
import {
  IconAlertCircle,
  IconArrowLeft,
  IconArrowRight,
  IconCheck,
  IconClipboardCheck,
  IconClipboardList,
  IconClock,
  IconFileText,
  IconHistory,
  IconNotes,
  IconPackage,
  IconPill,
  IconReceiptTax,
  IconStethoscope,
  IconUserPlus,
  IconX
} from "@tabler/icons-react";
import type { AppointmentResponse } from "@modules/agenda/types";
import type { Address, BloodType, EmergencyContact, Gender, MaritalStatus, PatientResponse } from "@modules/patients/types";
import type { QuotationResponse } from "@modules/treatments/quotationTypes";
import type { MaterialResponse } from "@modules/inventory/types";
import type { HistoryTemplateResponse, MedicalHistoryResponse } from "@modules/records/types";
import type { TreatmentCatalogItemResponse } from "@modules/treatments/types";
import {
  agendaApi,
  clinicalNotesApi,
  getFriendlyError,
  historyTemplatesApi,
  materialsApi,
  medicalHistoryApi,
  patientsApi,
  quotationsApi,
  ticketsApi,
  treatmentCatalogApi,
  visitsApi
} from "@shared/api/api";
import { PrescriptionSection } from "@modules/records/components/PrescriptionSection";
import { PatientHistoryPanel } from "@modules/records/components/PatientHistoryPanel";
import { PatientFields } from "@modules/patients/components/PatientFields";
import { HistoryFormModal } from "@modules/records/components/HistoryFormModal";
import { formatTimerSeconds } from "@shared/utils/appointmentTime";

type CareTab = "soap" | "prescriptions" | "materials" | "history_records";

type PlannedMaterialLine = {
  materialId?: string;
  materialName: string;
  estimatedQuantity: number;
  actualQuantity: number;
  unitOfMeasure?: string;
  procedureKey?: string;
  procedureName?: string;
};

type MaterialSection = {
  key: string;
  procedureName: string;
  lines: PlannedMaterialLine[];
};

export function PatientCareScreen({
  clinicId,
  patient: initialPatient,
  appointment,
  doctorId,
  onBack,
  onPatientCreated
}: {
  clinicId: string;
  patient: PatientResponse;
  appointment?: AppointmentResponse;
  doctorId?: string;
  onBack: () => void;
  onPatientCreated?: (created: PatientResponse) => void;
}) {
  const [currentPatient, setCurrentPatient] = useState<PatientResponse>(initialPatient);

  const isNewPatientPlaceholder =
    !currentPatient.id ||
    currentPatient.id.startsWith("temp") ||
    (currentPatient.firstName === "Paciente" && currentPatient.lastNamePaterno === "Nuevo");

  const isFirstTime =
    isNewPatientPlaceholder ||
    !appointment?.patientId ||
    Boolean(appointment?.reason?.toLowerCase().includes("primera"));

  // Step state: Step 1 (Register), Step 2 (Interrogation / Templates), Step 3 (Active Consultation)
  const [activeStep, setActiveStep] = useState<1 | 2 | 3>(isFirstTime ? 1 : 3);
  const [activeTab, setActiveTab] = useState<CareTab>("soap");

  const [hasHistory, setHasHistory] = useState<boolean | null>(null);
  const [loadingHistory, setLoadingHistory] = useState(true);

  // Live Timer State
  const [secondsElapsed, setSecondsElapsed] = useState(0);

  // Quick SOAP Notes Floating Drawer State
  const [quickNotesOpen, setQuickNotesOpen] = useState(false);

  // Step 1: Patient Registration State
  const [savingPatient, setSavingPatient] = useState(false);
  const [patientError, setPatientError] = useState("");

  // Step 2: Templates & History Form State
  const [templates, setTemplates] = useState<HistoryTemplateResponse[]>([]);
  const [loadingTemplates, setLoadingTemplates] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState<HistoryTemplateResponse | null>(null);
  const [savedHistories, setSavedHistories] = useState<MedicalHistoryResponse[]>([]);

  // Step 3: SOAP Notes state
  const [subjective, setSubjective] = useState("");
  const [objective, setObjective] = useState("");
  const [assessment, setAssessment] = useState("");
  const [plan, setPlan] = useState("");
  const [savingNote, setSavingNote] = useState(false);
  const [noteSuccess, setNoteSuccess] = useState("");
  const [noteError, setNoteError] = useState("");

  // Material & Emergency Catalog Services state
  const [catalogServices, setCatalogServices] = useState<TreatmentCatalogItemResponse[]>([]);
  const [appliedServices, setAppliedServices] = useState<Array<{ catalogItemId: string; name: string; price: number; quantity: number }>>([]);
  const [quotations, setQuotations] = useState<QuotationResponse[]>([]);
  const [selectedQuotationId, setSelectedQuotationId] = useState<string>("");
  const [materials, setMaterials] = useState<MaterialResponse[]>([]);
  const [usedMaterials, setUsedMaterials] = useState<Array<{ materialId: string; quantity: number }>>([]);
  const [plannedMaterials, setPlannedMaterials] = useState<PlannedMaterialLine[]>([]);
  const [additionalPlannedMaterials, setAdditionalPlannedMaterials] = useState<PlannedMaterialLine[]>([]);
  const [savingVisit, setSavingVisit] = useState(false);
  const [visitSuccess, setVisitSuccess] = useState("");

  // Finish Consultation Modal State
  const [finishModalOpen, setFinishModalOpen] = useState(false);
  const [createCashTicket, setCreateCashTicket] = useState(true);
  const [completingConsultation, setCompletingConsultation] = useState(false);
  const [finishError, setFinishError] = useState("");

  const patientFullName = [currentPatient.firstName, currentPatient.lastNamePaterno, currentPatient.lastNameMaterno]
    .filter(Boolean)
    .join(" ");

  // Timer effect
  useEffect(() => {
    const timer = setInterval(() => {
      setSecondsElapsed((prev) => prev + 1);
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // Load patient data, catalog & templates
  useEffect(() => {
    if (!clinicId || !currentPatient.id || isNewPatientPlaceholder) {
      setHasHistory(false);
      setLoadingHistory(false);
      return;
    }

    setLoadingHistory(true);
    setLoadingTemplates(true);

    const quotationRequest = appointment?.quotationId
      ? quotationsApi
        .get(currentPatient.id, appointment.quotationId, clinicId)
        .then((quotation) => [quotation])
        .catch(() => quotationsApi.listByPatient(currentPatient.id, clinicId).catch(() => []))
      : quotationsApi.listByPatient(currentPatient.id, clinicId).catch(() => []);

    Promise.all([
      historyTemplatesApi.list(clinicId, currentPatient.id),
      medicalHistoryApi.listByPatient(currentPatient.id, clinicId),
      quotationRequest,
      materialsApi.list(clinicId).catch(() => []),
      treatmentCatalogApi.list(clinicId).catch(() => [])
    ])
      .then(([templateList, historyList, quotationList, materialList, catalogList]) => {
        setTemplates(templateList.filter((t) => t.active));
        setSavedHistories(historyList);
        setHasHistory(historyList.length > 0);

        setQuotations(quotationList);
        const appointmentQuotation = appointment?.quotationId
          ? quotationList.find((quotation) => quotation.id === appointment.quotationId)
          : undefined;
        const quotationForConsultation = appointmentQuotation || quotationList[0];
        setSelectedQuotationId(quotationForConsultation?.id || "");

        const selectedQuotationItemIds = appointment?.quotationItemIds?.length
          ? appointment.quotationItemIds
          : appointment?.quotationItemId
            ? [appointment.quotationItemId]
            : [];
        const plannedItems = appointmentQuotation?.items.filter((item) => selectedQuotationItemIds.includes(item.id)) || [];
        setPlannedMaterials(
          plannedItems.flatMap((plannedItem) => (plannedItem.materials || []).map((line) => ({
              materialId: line.materialId,
              materialName: line.materialName,
              estimatedQuantity: line.estimatedQuantity,
              actualQuantity: 0,
              unitOfMeasure: materialList.find((material) => material.id === line.materialId)?.unitOfMeasure,
              procedureKey: `quotation:${plannedItem.id}`,
              procedureName: plannedItem.description
            })))
        );
        setMaterials(materialList);
        setCatalogServices(catalogList.filter((c) => c.active));

        // If patient already has histories, auto-advance to step 3
        if (historyList.length > 0 && activeStep === 2) {
          setActiveStep(3);
        }
      })
      .catch((err) => {
        setPatientError(getFriendlyError(err));
      })
      .finally(() => {
        setLoadingHistory(false);
        setLoadingTemplates(false);
      });
  }, [clinicId, currentPatient.id, isNewPatientPlaceholder]);

  // Handle Step 1: Submit Full Patient Registration Form (PatientFields)
  const handleSubmitPatientForm = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPatientError("");
    const form = new FormData(event.currentTarget);
    const value = (name: string) => String(form.get(name) ?? "").trim();

    const address: Address = {
      street: value("addressStreet") || undefined,
      outdoorNumber: value("addressOutdoorNumber") || undefined,
      indoorNumber: value("addressIndoorNumber") || undefined,
      colonia: value("addressColonia") || undefined,
      municipality: value("addressMunicipality") || undefined,
      state: value("addressState") || undefined,
      zipCode: value("addressZipCode") || undefined
    };

    const emergencyContact: EmergencyContact = {
      fullName: value("emergencyContactFullName") || undefined,
      relationship: value("emergencyContactRelationship") || undefined,
      phone: value("emergencyContactPhone") || undefined
    };

    const hasAddress = Object.values(address).some(Boolean);
    const hasEmergencyContact = Object.values(emergencyContact).some(Boolean);

    const draft = {
      firstName: value("firstName"),
      lastNamePaterno: value("lastNamePaterno"),
      lastNameMaterno: value("lastNameMaterno") || undefined,
      dateOfBirth: value("dateOfBirth"),
      gender: value("gender") as Gender,
      curp: value("curp") || undefined,
      phone: value("phone") || undefined,
      email: value("email") || undefined,
      occupation: value("occupation") || undefined,
      maritalStatus: (value("maritalStatus") as MaritalStatus) || undefined,
      nationality: value("nationality") || undefined,
      bloodType: (value("bloodType") as BloodType) || undefined,
      address: hasAddress ? address : undefined,
      emergencyContact: hasEmergencyContact ? emergencyContact : undefined
    };

    if (!draft.firstName || !draft.lastNamePaterno || !draft.dateOfBirth || !draft.gender || !draft.phone) {
      setPatientError("Por favor completa todos los campos requeridos (*).");
      return;
    }

    try {
      setSavingPatient(true);
      let registered: PatientResponse;

      if (isNewPatientPlaceholder) {
        registered = await patientsApi.register({ clinicId, ...draft });
      } else {
        registered = await patientsApi.update(currentPatient.id, draft);
      }

      setCurrentPatient(registered);
      onPatientCreated?.(registered);

      // Link new patient ID to appointment if available
      if (appointment?.id) {
        try {
          await agendaApi.assignPatient(clinicId, appointment.id, registered.id);
        } catch (e) {
          console.warn("Could not bind new patient to appointment:", e);
        }
      }

      // Load templates for newly registered patient
      const templateList = await historyTemplatesApi.list(clinicId, registered.id);
      setTemplates(templateList.filter((t) => t.active));

      setNoteSuccess("Paciente registrado exitosamente. Procede con el interrogatorio de Historia Clínica.");
      setActiveStep(2);
    } catch (err) {
      setPatientError(getFriendlyError(err));
    } finally {
      setSavingPatient(false);
    }
  };

  const handleSaveSoapNote = async () => {
    setNoteError("");
    setNoteSuccess("");
    if (!subjective.trim() && !objective.trim() && !assessment.trim() && !plan.trim()) {
      setNoteError("Ingresa al menos una sección de la nota clínica SOAP.");
      return;
    }
    try {
      setSavingNote(true);
      await clinicalNotesApi.create(currentPatient.id, {
        clinicId,
        doctorId: doctorId || appointment?.doctorStaffId || "00000000-0000-0000-0000-000000000000",
        status: "DRAFT",
        subjective,
        objective,
        assessment,
        plan
      });
      setNoteSuccess("Nota clínica guardada exitosamente.");
    } catch (err) {
      setNoteError(getFriendlyError(err));
    } finally {
      setSavingNote(false);
    }
  };

  // Add catalog service applied in session (Emergency / Ad-Hoc)
  const handleAddAppliedService = (catalogItemId: string, quantity: number) => {
    const serviceObj = catalogServices.find((s) => s.id === catalogItemId);
    if (!serviceObj) return;

    setAppliedServices((prev) => {
      const existing = prev.find((item) => item.catalogItemId === catalogItemId);
      if (existing) {
        return prev.map((item) => (item.catalogItemId === catalogItemId ? { ...item, quantity: item.quantity + quantity } : item));
      }
      return [...prev, { catalogItemId, name: serviceObj.name, price: serviceObj.defaultPrice ?? 0, quantity }];
    });

    setAdditionalPlannedMaterials((previous) => {
      const next = [...previous];
      serviceObj.materials.forEach((catalogMaterial) => {
        const estimatedQuantity = (catalogMaterial.typicalQuantity || 1) * quantity;
        const procedureKey = `catalog:${serviceObj.id}`;
        const existing = next.find((line) => line.materialId === catalogMaterial.materialId && line.procedureKey === procedureKey);
        if (existing) {
          existing.estimatedQuantity += estimatedQuantity;
          return;
        }
      next.push({
          materialId: catalogMaterial.materialId,
          materialName: catalogMaterial.materialName,
          estimatedQuantity,
          actualQuantity: 0,
          unitOfMeasure: materials.find((material) => material.id === catalogMaterial.materialId)?.unitOfMeasure,
          procedureKey,
          procedureName: serviceObj.name
        });
      });
      return next;
    });
  };

  const handleAddMaterialUsage = (materialId: string, quantity: number) => {
    setUsedMaterials((prev) => {
      const existing = prev.find((item) => item.materialId === materialId);
      if (existing) {
        return prev.map((item) => (item.materialId === materialId ? { ...item, quantity: item.quantity + quantity } : item));
      }
      return [...prev, { materialId, quantity }];
    });
  };

  const updatePlannedMaterialUsage = (procedureKey: string | undefined, materialName: string, quantity: number) => {
    setPlannedMaterials((previous) => previous.map((line) =>
      line.procedureKey === procedureKey && line.materialName === materialName
        ? { ...line, actualQuantity: Math.max(0, quantity) }
        : line
    ));
  };

  const updateAdditionalMaterialUsage = (procedureKey: string | undefined, materialId: string | undefined, materialName: string, quantity: number) => {
    setAdditionalPlannedMaterials((previous) => previous.map((line) =>
      line.procedureKey === procedureKey && line.materialId === materialId && line.materialName === materialName
        ? { ...line, actualQuantity: Math.max(0, quantity) }
        : line
    ));
  };

  const getEffectiveMaterialUsage = () => {
    const usageByMaterial = new Map<string, { materialId?: string; materialName: string; quantity: number; procedureKey?: string }>();

    [...plannedMaterials, ...additionalPlannedMaterials].forEach((line) => {
      if (line.actualQuantity <= 0) return;
      const materialKey = line.materialId || line.materialName;
      const key = `${line.procedureKey || "manual"}:${materialKey}`;
      usageByMaterial.set(key, {
        materialId: line.materialId,
        materialName: line.materialName,
        quantity: line.actualQuantity,
        procedureKey: line.procedureKey
      });
    });

    usedMaterials.forEach((line) => {
      const material = materials.find((item) => item.id === line.materialId);
      const key = `manual:${line.materialId}`;
      const previous = usageByMaterial.get(key);
      usageByMaterial.set(key, {
        materialId: line.materialId,
        materialName: material?.name || previous?.materialName || "Material",
        quantity: (previous?.quantity || 0) + line.quantity,
        procedureKey: previous?.procedureKey
      });
    });

    return Array.from(usageByMaterial.values());
  };

  const groupMaterialSections = (lines: PlannedMaterialLine[], emptySections: MaterialSection[] = []): MaterialSection[] => {
    const sections = new Map<string, MaterialSection>();
    emptySections.forEach((section) => sections.set(section.key, section));
    lines.forEach((line) => {
      const key = line.procedureKey || "other";
      const current = sections.get(key) || {
        key,
        procedureName: line.procedureName || "Materiales adicionales",
        lines: []
      };
      current.lines.push(line);
      sections.set(key, current);
    });
    return Array.from(sections.values());
  };

  const additionalMaterialSections = groupMaterialSections(
    additionalPlannedMaterials,
    appliedServices.map((service) => ({
      key: `catalog:${service.catalogItemId}`,
      procedureName: service.name,
      lines: []
    }))
  );

  const handleSaveVisitMaterials = async () => {
    setVisitSuccess("");
    const effectiveMaterialUsage = getEffectiveMaterialUsage();
    if (effectiveMaterialUsage.length === 0 && appliedServices.length === 0) return;
    try {
      setSavingVisit(true);
      const visitPayload = {
        clinicId,
        doctorId,
        notes: `Procedimientos e insumos de consulta ${appointment ? `- Cita #${appointment.id.slice(0, 6)}` : ""}`,
        items: [{
          quotationItemId: appointment?.quotationItemIds?.[0] || appointment?.quotationItemId,
          materialsUsed: effectiveMaterialUsage.map((line) => ({
            materialId: line.materialId,
            materialName: line.materialName,
            actualQuantity: line.quantity
          }))
        }]
      };

      if (selectedQuotationId) {
        await visitsApi.create(currentPatient.id, selectedQuotationId, visitPayload as any);
      } else {
        await visitsApi.createGeneral(currentPatient.id, visitPayload as any);
      }
      setVisitSuccess("Procedimientos e insumos registrados exitosamente.");
    } catch (err) {
      setNoteError(getFriendlyError(err));
    } finally {
      setSavingVisit(false);
    }
  };

  // Complete consultation & create cash ticket if selected
  const handleConfirmFinishConsultation = async () => {
    let completionBlocked = false;
    setFinishError("");
    try {
      setCompletingConsultation(true);

      // Create Ticket in Caja if selected and services/items are registered
      const effectiveMaterialUsage = getEffectiveMaterialUsage();
      const hasBillableActivity = Boolean(plannedQuotationItem || appliedServices.length > 0 || effectiveMaterialUsage.length > 0);
      if (createCashTicket && currentPatient.id && hasBillableActivity) {
        const createdByStaffId = doctorId || appointment?.doctorStaffId;
        if (!createdByStaffId) {
          completionBlocked = true;
          setFinishError("No se pudo identificar al empleado responsable de la consulta para enviarla a Caja.");
          return;
        }

        const ticketItems = [
          ...plannedQuotationItems.map((item) => ({
            concept: item.description,
            unitPrice: item.laborCharge,
            quantity: 1,
            totalPrice: item.laborCharge
          })),
          ...appliedServices.map((s) => ({
            concept: s.name,
            unitPrice: s.price,
            quantity: s.quantity,
            totalPrice: s.price * s.quantity
          })),
          ...effectiveMaterialUsage.map((m) => {
            const mat = materials.find((item) => item.id === m.materialId);
            const price = mat?.unitCost || 0;
            return {
              concept: `Insumo: ${m.materialName}`,
              unitPrice: price,
              quantity: m.quantity,
              totalPrice: price * m.quantity
            };
          })
        ];

        const grandTotal = ticketItems.reduce((acc, curr) => acc + curr.totalPrice, 0);
        if (grandTotal <= 0) {
          completionBlocked = true;
          setFinishError("No se puede enviar la consulta a Caja porque el importe calculado es cero.");
          return;
        }

        try {
          await ticketsApi.register(clinicId, {
            patientId: currentPatient.id,
            quotationId: appointment?.quotationId,
            concept: `Consulta médica: ${plannedQuotationItem?.description || "Servicios e insumos"}`,
            createdByStaffId,
            paymentLines: [{
              method: "CASH",
              amount: grandTotal
            }]
          });
        } catch (e) {
          completionBlocked = true;
          setFinishError(`No se pudo enviar la consulta a Caja: ${getFriendlyError(e)}`);
          return;
        }
      }

      // Transition appointment status to COMPLETED
      if (appointment?.id && clinicId) {
        await agendaApi.transitionStatus(clinicId, appointment.id, "COMPLETED");
      }
    } catch (err) {
      completionBlocked = true;
      setFinishError(getFriendlyError(err));
    } finally {
      setCompletingConsultation(false);
      if (!completionBlocked) {
        setFinishModalOpen(false);
        onBack();
      }
    }
  };

  const activeQuotation = quotations.find((q) => q.id === selectedQuotationId);
  const selectedQuotationItemIds = appointment?.quotationItemIds?.length
    ? appointment.quotationItemIds
    : appointment?.quotationItemId
      ? [appointment.quotationItemId]
      : [];
  const plannedQuotationItems = activeQuotation?.items.filter((item) => selectedQuotationItemIds.includes(item.id)) || [];
  const plannedQuotationItem = plannedQuotationItems[0];
  const effectiveMaterialUsage = getEffectiveMaterialUsage();
  const hasBillableActivity = Boolean(plannedQuotationItem || appliedServices.length > 0 || effectiveMaterialUsage.length > 0);
  const totalAppliedServicesAmount = appliedServices.reduce((acc, curr) => acc + curr.price * curr.quantity, 0);

  return (
    <div
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        zIndex: 9999,
        backgroundColor: "var(--color-surface)",
        color: "var(--color-text-1)",
        overflowY: "auto",
        display: "flex",
        flexDirection: "column"
      }}
    >
      {/* Top Session Header - Clinerya Dark Teal Theme */}
      <div
        style={{
          backgroundColor: "var(--color-sidebar)",
          color: "#ffffff",
          padding: "var(--space-3) var(--space-6)",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          boxShadow: "0 2px 8px rgba(0,0,0,0.12)",
          position: "sticky",
          top: 0,
          zIndex: 10
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "var(--space-4)" }}>
          <button
            type="button"
            className="icon-btn"
            onClick={onBack}
            style={{ backgroundColor: "rgba(255,255,255,0.1)", borderColor: "transparent", color: "#ffffff" }}
            title="Salir de la estación de consulta"
          >
            <IconX size={20} />
          </button>
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: "var(--space-2)" }}>
              <h2 style={{ margin: 0, fontSize: "18px", fontWeight: 600, color: "#ffffff" }}>
                Estación de Consulta: {patientFullName || "Paciente Nuevo"}
              </h2>
              {isFirstTime && (
                <span className="badge warning" style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                  <IconAlertCircle size={13} /> Primera Vez
                </span>
              )}
            </div>
            <span style={{ fontSize: "12px", color: "var(--color-accent)", opacity: 0.9 }}>
              CURP: {currentPatient.curp || "Sin registrar"} | Tel: {currentPatient.phone || "Sin registrar"}
              {appointment && ` | Cita: ${new Date(appointment.scheduledStart).toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit" })}`}
            </span>
          </div>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: "var(--space-4)" }}>
          {/* Quick Notes Toggle */}
          <button
            type="button"
            className="btn ghost"
            onClick={() => setQuickNotesOpen(!quickNotesOpen)}
            style={{
              borderColor: "var(--color-accent)",
              color: "var(--color-accent)",
              borderWidth: "1px",
              borderStyle: "solid"
            }}
          >
            <IconNotes size={16} /> {quickNotesOpen ? "Cerrar Notas SOAP" : "Notas Rápidas SOAP"}
          </button>

          {/* Live Timer Pill */}
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "var(--space-2)",
              backgroundColor: "rgba(255, 255, 255, 0.12)",
              padding: "4px var(--space-4)",
              borderRadius: "20px",
              border: "1px solid rgba(255, 255, 255, 0.2)",
              fontSize: "13px",
              fontWeight: 600,
              color: "#ffffff"
            }}
          >
            <IconClock size={16} />
            <span>Tiempo: {formatTimerSeconds(secondsElapsed)}</span>
          </div>

          <button
            type="button"
            className="btn primary"
            onClick={() => {
              setFinishError("");
              setFinishModalOpen(true);
            }}
            style={{ backgroundColor: "var(--color-success)", borderColor: "var(--color-success)", color: "#ffffff" }}
          >
            <IconCheck size={18} /> Finalizar Consulta
          </button>
        </div>
      </div>

      {/* Sequential Stepper Header (Exclusivo Primera Vez) */}
      {isFirstTime && (
        <div
          style={{
            backgroundColor: "var(--color-card)",
            borderBottom: "1px solid var(--color-border)",
            padding: "var(--space-3) var(--space-6)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            gap: "var(--space-8)"
          }}
        >
          <div
            onClick={() => setActiveStep(1)}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "var(--space-2)",
              cursor: "pointer",
              opacity: activeStep === 1 ? 1 : 0.65
            }}
          >
            <span
              style={{
                width: "28px",
                height: "28px",
                borderRadius: "50%",
                backgroundColor: activeStep === 1 ? "var(--color-primary)" : "var(--color-border)",
                color: activeStep === 1 ? "#ffffff" : "var(--color-text-2)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontWeight: 700,
                fontSize: "13px"
              }}
            >
              1
            </span>
            <span style={{ fontWeight: activeStep === 1 ? 700 : 500, fontSize: "14px", color: activeStep === 1 ? "var(--color-primary)" : "var(--color-text-2)" }}>
              1. Registro del Paciente Nuevo
            </span>
          </div>

          <IconArrowRight size={18} style={{ color: "var(--color-text-3)" }} />

          <div
            onClick={() => {
              if (!isNewPatientPlaceholder) setActiveStep(2);
            }}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "var(--space-2)",
              cursor: isNewPatientPlaceholder ? "not-allowed" : "pointer",
              opacity: activeStep === 2 ? 1 : 0.65
            }}
          >
            <span
              style={{
                width: "28px",
                height: "28px",
                borderRadius: "50%",
                backgroundColor: activeStep === 2 ? "var(--color-primary)" : "var(--color-border)",
                color: activeStep === 2 ? "#ffffff" : "var(--color-text-2)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontWeight: 700,
                fontSize: "13px"
              }}
            >
              2
            </span>
            <span style={{ fontWeight: activeStep === 2 ? 700 : 500, fontSize: "14px", color: activeStep === 2 ? "var(--color-primary)" : "var(--color-text-2)" }}>
              2. Iniciar Historia Clínica (Interrogatorio)
            </span>
          </div>

          <IconArrowRight size={18} style={{ color: "var(--color-text-3)" }} />

          <div
            onClick={() => {
              if (!isNewPatientPlaceholder) setActiveStep(3);
            }}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "var(--space-2)",
              cursor: isNewPatientPlaceholder ? "not-allowed" : "pointer",
              opacity: activeStep === 3 ? 1 : 0.65
            }}
          >
            <span
              style={{
                width: "28px",
                height: "28px",
                borderRadius: "50%",
                backgroundColor: activeStep === 3 ? "var(--color-primary)" : "var(--color-border)",
                color: activeStep === 3 ? "#ffffff" : "var(--color-text-2)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontWeight: 700,
                fontSize: "13px"
              }}
            >
              3
            </span>
            <span style={{ fontWeight: activeStep === 3 ? 700 : 500, fontSize: "14px", color: activeStep === 3 ? "var(--color-primary)" : "var(--color-text-2)" }}>
              3. Consulta y Herramientas (SOAP, Recetas, Insumos)
            </span>
          </div>
        </div>
      )}

      <div style={{ padding: "var(--space-5)", flex: 1, position: "relative" }}>
        {noteSuccess && <div className="banner success" style={{ marginBottom: "var(--space-4)" }}>{noteSuccess}</div>}
        {noteError && <div className="banner danger" style={{ marginBottom: "var(--space-4)" }}>{noteError}</div>}
        {patientError && <div className="banner danger" style={{ marginBottom: "var(--space-4)" }}>{patientError}</div>}

        {/* PASO 1: Registro Completo del Paciente Nuevo (Exclusivo Primera Vez) */}
        {activeStep === 1 && (
          <div className="panel full" style={{ padding: "var(--space-6)", maxWidth: "900px", margin: "0 auto" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "var(--space-3)", marginBottom: "var(--space-5)" }}>
              <IconUserPlus size={28} style={{ color: "var(--color-primary)" }} />
              <div>
                <h2 style={{ margin: 0, fontSize: "18px" }}>Paso 1: Registro e Identificación del Paciente Nuevo</h2>
                <span style={{ fontSize: "13px", color: "var(--color-text-2)" }}>
                  Por norma médica, completa la ficha de registro para aperturar el expediente oficial en el sistema.
                </span>
              </div>
            </div>

            <form className="profile-form" onSubmit={handleSubmitPatientForm}>
              <PatientFields patient={currentPatient} />

              <div style={{ display: "flex", justifyContent: "flex-end", gap: "var(--space-3)", marginTop: "var(--space-6)" }}>
                <button
                  type="submit"
                  className="btn primary"
                  disabled={savingPatient}
                >
                  <IconArrowRight size={18} />
                  {savingPatient ? "Guardando Paciente..." : "Guardar Paciente y Pasar al Interrogatorio"}
                </button>
              </div>
            </form>
          </div>
        )}

        {/* PASO 2: Iniciar Historia Clínica / Interrogatorio en Página Completa Inline */}
        {activeStep === 2 && (
          <div className="panel full" style={{ padding: "var(--space-6)" }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "var(--space-5)" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "var(--space-3)" }}>
                <IconClipboardCheck size={28} style={{ color: "var(--color-primary)" }} />
                <div>
                  <h2 style={{ margin: 0, fontSize: "18px" }}>Paso 2: Iniciar Historia Clínica (Interrogatorio Médico)</h2>
                  <span style={{ fontSize: "13px", color: "var(--color-text-2)" }}>
                    Selecciona una plantilla de historia clínica para contestar el interrogatorio directamente en pantalla.
                  </span>
                </div>
              </div>
              <button
                type="button"
                className="btn secondary"
                onClick={() => setActiveStep(3)}
              >
                Saltar a Consulta SOAP <IconArrowRight size={16} />
              </button>
            </div>

            {selectedTemplate ? (
              <div>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "var(--space-4)" }}>
                  <button
                    type="button"
                    className="btn secondary"
                    onClick={() => setSelectedTemplate(null)}
                  >
                    <IconArrowLeft size={16} /> Volver al Catálogo de Plantillas
                  </button>
                  <button
                    type="button"
                    className="btn primary"
                    onClick={() => setActiveStep(3)}
                  >
                    Pasar a Consulta SOAP <IconArrowRight size={16} />
                  </button>
                </div>

                <HistoryFormModal
                  template={selectedTemplate}
                  clinicId={clinicId}
                  patient={currentPatient}
                  embedded={true}
                  onClose={() => setSelectedTemplate(null)}
                  onSaved={() => {
                    setSelectedTemplate(null);
                    setHasHistory(true);
                    setActiveStep(3);
                    setNoteSuccess("Historia clínica completada e interrogatorio guardado exitosamente.");
                  }}
                />
              </div>
            ) : loadingTemplates ? (
              <p style={{ color: "var(--color-text-2)" }}>Cargando plantillas de historia clínica...</p>
            ) : (
              <div>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "var(--space-4)", marginBottom: "var(--space-6)" }}>
                  {templates.map((template) => (
                    <div
                      key={template.id}
                      className="panel"
                      style={{
                        padding: "var(--space-5)",
                        display: "flex",
                        flexDirection: "column",
                        justifyContent: "space-between"
                      }}
                    >
                      <div>
                        <div style={{ display: "flex", alignItems: "center", gap: "var(--space-2)", marginBottom: "var(--space-2)" }}>
                          <IconFileText size={22} style={{ color: "var(--color-primary)" }} />
                          <h4 style={{ margin: 0, fontSize: "15px" }}>{template.name}</h4>
                        </div>
                        <p style={{ fontSize: "12px", color: "var(--color-text-2)", margin: 0 }}>
                          {template.description || "Plantilla médica estándar de interrogatorio y antecedentes."}
                        </p>
                      </div>

                      <button
                        type="button"
                        className="btn primary"
                        onClick={() => setSelectedTemplate(template)}
                        style={{ marginTop: "var(--space-4)", width: "100%" }}
                      >
                        <IconClipboardList size={16} /> Iniciar Interrogatorio
                      </button>
                    </div>
                  ))}
                  {templates.length === 0 && (
                    <div className="banner neutral" style={{ gridColumn: "1 / -1" }}>
                      No hay plantillas activas registradas. Puedes continuar directamente con la consulta.
                    </div>
                  )}
                </div>

                {savedHistories.length > 0 && (
                  <div style={{ marginTop: "var(--space-6)" }}>
                    <h4>Historias Clínicas ya registradas para este paciente:</h4>
                    <div className="table-wrapper">
                      <table className="data-table">
                        <thead>
                          <tr>
                            <th>Fecha</th>
                            <th>Plantilla</th>
                            <th>Estado</th>
                          </tr>
                        </thead>
                        <tbody>
                          {savedHistories.map((h) => (
                            <tr key={h.id}>
                              <td>{new Date(h.updatedAt).toLocaleDateString("es-MX")}</td>
                              <td>{templates.find((t) => t.id === h.templateId)?.name || "Historia Médica"}</td>
                              <td><span className="badge success">Completada</span></td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* PASO 3: Consulta Activa / Herramientas Clínicas (SOAP, Recetas e Insumos) */}
        {activeStep === 3 && (
          <>
            {/* Navigation Tabs - Clinerya Harmonized */}
            <div style={{ display: "flex", borderBottom: "1px solid var(--color-border)", gap: "var(--space-2)", marginBottom: "var(--space-4)" }}>
              <button
                type="button"
                className={`btn ${activeTab === "soap" ? "primary" : "ghost"}`}
                onClick={() => setActiveTab("soap")}
              >
                <IconFileText size={18} /> Nota de Atención (SOAP)
              </button>

              <button
                type="button"
                className={`btn ${activeTab === "prescriptions" ? "primary" : "ghost"}`}
                onClick={() => setActiveTab("prescriptions")}
              >
                <IconPill size={18} /> Recetas Médicas
              </button>

              <button
                type="button"
                className={`btn ${activeTab === "materials" ? "primary" : "ghost"}`}
                onClick={() => setActiveTab("materials")}
              >
                <IconPackage size={18} /> Materiales y Servicios (Planeado vs Real)
              </button>

              <button
                type="button"
                className={`btn ${activeTab === "history_records" ? "primary" : "ghost"}`}
                onClick={() => setActiveTab("history_records")}
              >
                <IconHistory size={18} /> Historia Clínica del Paciente
              </button>
            </div>

            {/* TAB 1: Nota SOAP */}
            {activeTab === "soap" && (
              <div className="panel full" style={{ padding: "var(--space-5)" }}>
                <h3 style={{ margin: "0 0 var(--space-4) 0", fontSize: "16px" }}>Registro de Nota Clínica SOAP de la Cita</h3>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "var(--space-5)" }}>
                  <div className="field">
                    <label>S - Subjetivo (Motivo de consulta y síntomas):</label>
                    <textarea
                      rows={5}
                      placeholder="Descripción expresada por el paciente..."
                      value={subjective}
                      onChange={(e) => setSubjective(e.target.value)}
                    />
                  </div>

                  <div className="field">
                    <label>O - Objetivo (Exploración física y hallazgos):</label>
                    <textarea
                      rows={5}
                      placeholder="Hallazgos observados, signos clínicos..."
                      value={objective}
                      onChange={(e) => setObjective(e.target.value)}
                    />
                  </div>

                  <div className="field">
                    <label>A - Evaluación / Diagnóstico Presuntivo:</label>
                    <textarea
                      rows={5}
                      placeholder="Diagnóstico médico o estado de evolución..."
                      value={assessment}
                      onChange={(e) => setAssessment(e.target.value)}
                    />
                  </div>

                  <div className="field">
                    <label>P - Plan de Tratamiento e Indicaciones:</label>
                    <textarea
                      rows={5}
                      placeholder="Procedimientos a realizar, recomendaciones..."
                      value={plan}
                      onChange={(e) => setPlan(e.target.value)}
                    />
                  </div>
                </div>

                <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "var(--space-5)" }}>
                  <button type="button" className="btn primary" onClick={handleSaveSoapNote} disabled={savingNote}>
                    {savingNote ? "Guardando..." : "Guardar Nota Clínica"}
                  </button>
                </div>
              </div>
            )}

            {/* TAB 2: Recetas */}
            {activeTab === "prescriptions" && (
              <PrescriptionSection
                clinicId={clinicId}
                patient={currentPatient}
                appointmentId={appointment?.id}
                doctorId={doctorId}
              />
            )}

            {/* TAB 3: Materiales e Insumos y Servicios de Emergencia del Catálogo */}
            {activeTab === "materials" && (
              <>
                {plannedQuotationItem && (
                  <div className="panel full" style={{ marginBottom: "var(--space-5)" }}>
                    <div className="panel-heading">
                      <div>
                        <h3 style={{ fontSize: "15px", display: "flex", alignItems: "center", gap: "var(--space-2)", marginBottom: "4px" }}>
                          <IconClipboardCheck size={20} style={{ color: "var(--color-primary)" }} /> Tratamiento programado para esta consulta
                        </h3>
                        <span style={{ fontSize: "12px", color: "var(--color-text-2)" }}>
                          Verifica lo estimado en la cotizaciÃ³n contra lo utilizado durante la atenciÃ³n.
                        </span>
                      </div>
                      <span className={`badge ${plannedQuotationItem.progressStatus === "COMPLETED" ? "success" : "neutral"}`}>
                        {plannedQuotationItems.map((item) => item.description).join(" + ")}
                      </span>
                    </div>

                    <div style={{ padding: "var(--space-4)" }}>
                      <div style={{ display: "flex", gap: "var(--space-5)", flexWrap: "wrap", marginBottom: "var(--space-4)", fontSize: "13px" }}>
                        <span><strong>Procedimientos:</strong> {plannedQuotationItems.map((item) => item.description).join(" + ")}</span>
                        <span><strong>Estado:</strong> {plannedQuotationItems.every((item) => item.progressStatus === "COMPLETED") ? "Completados" : "Pendientes de atención"}</span>
                      </div>

                      <table className="data-table">
                        <thead>
                          <tr>
                            <th>Insumo planeado</th>
                            <th>Estimado</th>
                            <th>Utilizado</th>
                            <th>Diferencia</th>
                            <th>Cuadre</th>
                          </tr>
                        </thead>
                        <tbody>
                          {plannedMaterials.map((line, index) => {
                            const difference = line.actualQuantity - line.estimatedQuantity;
                            const isBalanced = difference === 0;
                            return (
                              <tr key={`${line.materialId || line.materialName}-${index}`}>
                                <td><strong>{line.materialName}</strong></td>
                                <td>{line.estimatedQuantity} {line.unitOfMeasure || "unid."}</td>
                                <td>
                                  <input
                                    type="number"
                                    min={0}
                                    step="any"
                                    value={line.actualQuantity}
                                    onChange={(event) => updatePlannedMaterialUsage(line.procedureKey, line.materialName, Number(event.target.value) || 0)}
                                    style={{ width: "100px" }}
                                    aria-label={`Cantidad utilizada de ${line.materialName}`}
                                  /> {line.unitOfMeasure || "unid."}
                                </td>
                                <td>{difference > 0 ? "+" : ""}{difference} {line.unitOfMeasure || "unid."}</td>
                                <td>
                                  <span className={`badge ${isBalanced ? "success" : difference < 0 ? "warning" : "danger"}`}>
                                    {isBalanced ? "Cuadrado" : difference < 0 ? "Faltante" : "Excedente"}
                                  </span>
                                </td>
                              </tr>
                            );
                          })}
                          {plannedMaterials.length === 0 && (
                            <tr>
                              <td colSpan={5}>El tratamiento no tiene insumos estimados.</td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "var(--space-5)" }}>
                {/* Panel 1: Servicios de Emergencia / Ad-Hoc aplicados en la consulta */}
                <div className="panel full">
                  <div className="panel-heading">
                    <h3 style={{ fontSize: "15px", display: "flex", alignItems: "center", gap: "var(--space-2)" }}>
                      <IconStethoscope size={20} style={{ color: "var(--color-primary)" }} /> Servicios y Procedimientos (Ad-Hoc / Emergencias)
                    </h3>
                  </div>
                  <div style={{ padding: "var(--space-4)" }}>
                    <p style={{ fontSize: "12px", color: "var(--color-text-2)", margin: "0 0 var(--space-3) 0" }}>
                      Registra servicios o procedimientos ejecutados directamente en esta consulta (sin depender de un tratamiento previo).
                    </p>

                    <div style={{ display: "flex", gap: "var(--space-2)", marginBottom: "var(--space-4)" }}>
                      <select id="select-catalog-service-input" style={{ flex: 1 }}>
                        <option value="">Seleccionar servicio del catálogo...</option>
                        {catalogServices.map((service) => (
                          <option key={service.id} value={service.id}>
                            {service.name} - {priceLabel(service)} {service.category ? `(${service.category})` : ""}
                          </option>
                        ))}
                      </select>
                      <input id="select-service-qty-input" type="number" defaultValue={1} min={1} style={{ width: "70px" }} />
                      <button
                        type="button"
                        className="btn primary"
                        onClick={() => {
                          const servSelect = document.getElementById("select-catalog-service-input") as HTMLSelectElement;
                          const qtyInput = document.getElementById("select-service-qty-input") as HTMLInputElement;
                          if (servSelect?.value && qtyInput?.value) {
                            handleAddAppliedService(servSelect.value, Number(qtyInput.value));
                          }
                        }}
                      >
                        + Agregar
                      </button>
                    </div>

                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Servicio / Procedimiento</th>
                          <th>Precio U.</th>
                          <th>Cant.</th>
                          <th>Subtotal</th>
                        </tr>
                      </thead>
                      <tbody>
                        {appliedServices.map((s) => (
                          <tr key={s.catalogItemId}>
                            <td><strong>{s.name}</strong></td>
                            <td>${s.price}</td>
                            <td>{s.quantity}</td>
                            <td><strong>${s.price * s.quantity}</strong></td>
                          </tr>
                        ))}
                        {appliedServices.length === 0 && (
                          <tr>
                            <td colSpan={4}>No has registrado servicios ad-hoc en esta sesión.</td>
                          </tr>
                        )}
                      </tbody>
                    </table>

                    {appliedServices.length > 0 && (
                      <div style={{ marginTop: "var(--space-3)", textAlign: "right", fontWeight: 700, fontSize: "14px", color: "var(--color-text-1)" }}>
                        Total Servicios: ${totalAppliedServicesAmount}
                      </div>
                    )}
                  </div>
                </div>

                {/* Panel 2: Materiales e Insumos consumidos */}
                <div className="panel full">
                  <div className="panel-heading">
                    <h3 style={{ fontSize: "15px", display: "flex", alignItems: "center", gap: "var(--space-2)" }}>
                      <IconPackage size={20} style={{ color: "var(--color-primary)" }} /> Insumos y Materiales Consumidos
                    </h3>
                  </div>
                  <div style={{ padding: "var(--space-4)" }}>
                    {visitSuccess && <div className="banner success">{visitSuccess}</div>}

                    {additionalMaterialSections.length > 0 && (
                      <div style={{ marginBottom: "var(--space-5)" }}>
                        <p style={{ fontSize: "12px", color: "var(--color-text-2)", margin: "0 0 var(--space-3) 0" }}>
                          Materiales sugeridos por los procedimientos adicionales seleccionados. Confirma la cantidad utilizada.
                        </p>
                        {additionalMaterialSections.map((section) => (
                          <div key={section.key} style={{ marginBottom: "var(--space-4)", border: "1px solid var(--color-border)", borderRadius: "6px", padding: "var(--space-3)" }}>
                            <h4 style={{ margin: "0 0 var(--space-3) 0", fontSize: "13px", color: "var(--color-text-1)" }}>
                              Materiales de {section.procedureName}
                            </h4>
                            <table className="data-table">
                          <thead>
                            <tr>
                              <th>Material sugerido</th>
                              <th>Estimado</th>
                              <th>Utilizado</th>
                              <th>Cuadre</th>
                            </tr>
                          </thead>
                            <tbody>
                              {section.lines.map((line, index) => {
                              const difference = line.actualQuantity - line.estimatedQuantity;
                              return (
                                <tr key={`${line.materialId || line.materialName}-${index}`}>
                                  <td><strong>{line.materialName}</strong></td>
                                  <td>{line.estimatedQuantity} {line.unitOfMeasure || "unid."}</td>
                                  <td>
                                    <input
                                      type="number"
                                      min={0}
                                      step="any"
                                      value={line.actualQuantity}
                                      onChange={(event) => updateAdditionalMaterialUsage(line.procedureKey, line.materialId, line.materialName, Number(event.target.value) || 0)}
                                      style={{ width: "100px" }}
                                      aria-label={`Cantidad utilizada de ${line.materialName}`}
                                    /> {line.unitOfMeasure || "unid."}
                                  </td>
                                  <td>
                                    <span className={`badge ${difference === 0 ? "success" : difference < 0 ? "warning" : "danger"}`}>
                                      {difference === 0 ? "Cuadrado" : difference < 0 ? "Faltante" : "Excedente"}
                                    </span>
                                  </td>
                                </tr>
                              );
                            })}
                            </tbody>
                            </table>
                            {section.lines.length === 0 && (
                              <p style={{ margin: 0, fontSize: "12px", color: "var(--color-text-2)" }}>
                                Este procedimiento no tiene materiales configurados.
                              </p>
                            )}
                          </div>
                        ))}
                      </div>
                    )}

                    <div style={{ display: "flex", gap: "var(--space-2)", marginBottom: "var(--space-4)" }}>
                      <select id="select-mat-input" style={{ flex: 1 }}>
                        <option value="">Seleccionar material del catálogo...</option>
                        {materials.map((m) => (
                          <option key={m.id} value={m.id}>
                            {m.name} (Stock: {m.currentStock} {m.unitOfMeasure})
                          </option>
                        ))}
                      </select>
                      <input id="select-qty-input" type="number" defaultValue={1} min={1} style={{ width: "80px" }} />
                      <button
                        type="button"
                        className="btn secondary"
                        onClick={() => {
                          const matSelect = document.getElementById("select-mat-input") as HTMLSelectElement;
                          const qtyInput = document.getElementById("select-qty-input") as HTMLInputElement;
                          if (matSelect?.value && qtyInput?.value) {
                            handleAddMaterialUsage(matSelect.value, Number(qtyInput.value));
                          }
                        }}
                      >
                        <IconClipboardList size={16} /> Registrar Consumo
                      </button>
                    </div>

                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Material</th>
                          <th>Cantidad Utilizada</th>
                        </tr>
                      </thead>
                      <tbody>
                        {usedMaterials.map((m) => {
                          const matObj = materials.find((mat) => mat.id === m.materialId);
                          return (
                            <tr key={m.materialId}>
                              <td><strong>{matObj?.name || m.materialId}</strong></td>
                              <td>{m.quantity} {matObj?.unitOfMeasure || "unid."}</td>
                            </tr>
                          );
                        })}
                        {usedMaterials.length === 0 && (
                          <tr>
                            <td colSpan={2}>No has añadido insumos consumidos en esta sesión.</td>
                          </tr>
                        )}
                      </tbody>
                    </table>

                    {(effectiveMaterialUsage.length > 0 || appliedServices.length > 0) && (
                      <div style={{ marginTop: "var(--space-4)", display: "flex", justifyContent: "flex-end" }}>
                        <button type="button" className="btn primary" onClick={handleSaveVisitMaterials} disabled={savingVisit}>
                          {savingVisit ? "Guardando..." : "Guardar Registro de Consumo"}
                        </button>
                      </div>
                    )}
                  </div>
                </div>
                </div>
              </>
            )}

            {/* TAB 4: Historia Clínica / Expediente del Paciente */}
            {activeTab === "history_records" && (
              <PatientHistoryPanel patient={currentPatient} clinicId={clinicId} onChangePatient={onBack} historyReadOnly={false} />
            )}
          </>
        )}
      </div>

      {/* Floating Quick SOAP Notes Side Drawer Toggle Button */}
      <button
        type="button"
        className="btn primary"
        onClick={() => setQuickNotesOpen(!quickNotesOpen)}
        style={{
          position: "fixed",
          bottom: "24px",
          right: "24px",
          zIndex: 10000,
          backgroundColor: "var(--color-sidebar)",
          borderColor: "var(--color-sidebar)",
          borderRadius: "24px",
          padding: "var(--space-3) var(--space-5)",
          boxShadow: "0 4px 16px rgba(39, 78, 78, 0.4)",
          fontWeight: 600,
          color: "#ffffff"
        }}
      >
        <IconNotes size={18} /> {quickNotesOpen ? "Cerrar Notas SOAP" : "📝 Notas Rápidas SOAP"}
      </button>

      {/* Quick SOAP Notes Side Drawer Panel */}
      {quickNotesOpen && (
        <div
          style={{
            position: "fixed",
            top: 56,
            right: 0,
            bottom: 0,
            width: "380px",
            zIndex: 10001,
            backgroundColor: "var(--color-card)",
            boxShadow: "-4px 0 20px rgba(0,0,0,0.15)",
            borderLeft: "2px solid var(--color-primary)",
            display: "flex",
            flexDirection: "column",
            padding: "var(--space-5)"
          }}
        >
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "var(--space-4)" }}>
            <h3 style={{ margin: 0, fontSize: "15px", display: "flex", alignItems: "center", gap: "var(--space-2)" }}>
              <IconNotes size={20} style={{ color: "var(--color-primary)" }} /> Notas Rápidas SOAP de Sesión
            </h3>
            <button type="button" className="icon-btn" onClick={() => setQuickNotesOpen(false)}>
              <IconX size={18} />
            </button>
          </div>

          <div style={{ flex: 1, overflowY: "auto", display: "flex", flexDirection: "column", gap: "var(--space-3)" }}>
            <div className="field">
              <label>S - Subjetivo (Síntomas expresados):</label>
              <textarea
                rows={3}
                value={subjective}
                onChange={(e) => setSubjective(e.target.value)}
                placeholder="Anotación inmediata..."
              />
            </div>
            <div className="field">
              <label>O - Objetivo (Exploración / Signos):</label>
              <textarea
                rows={3}
                value={objective}
                onChange={(e) => setObjective(e.target.value)}
                placeholder="Anotación inmediata..."
              />
            </div>
            <div className="field">
              <label>A - Evaluación / Diagnóstico:</label>
              <textarea
                rows={3}
                value={assessment}
                onChange={(e) => setAssessment(e.target.value)}
                placeholder="Anotación inmediata..."
              />
            </div>
            <div className="field">
              <label>P - Plan de Tratamiento:</label>
              <textarea
                rows={3}
                value={plan}
                onChange={(e) => setPlan(e.target.value)}
                placeholder="Anotación inmediata..."
              />
            </div>
          </div>

          <div style={{ marginTop: "var(--space-4)" }}>
            <button
              type="button"
              className="btn primary"
              onClick={handleSaveSoapNote}
              disabled={savingNote}
              style={{ width: "100%" }}
            >
              {savingNote ? "Guardando..." : "Guardar Nota SOAP en Expediente"}
            </button>
          </div>
        </div>
      )}

      {/* Finish Consultation & Create Cash Ticket Modal */}
      {finishModalOpen && (
        <div className="modal-overlay" onClick={() => setFinishModalOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: "520px" }}>
            <div className="panel-heading">
              <div style={{ display: "flex", alignItems: "center", gap: "var(--space-2)" }}>
                <IconCheck size={22} style={{ color: "var(--color-success)" }} />
                <h2>Finalizar Consulta Médica</h2>
              </div>
              <button className="icon-btn" type="button" onClick={() => setFinishModalOpen(false)}>
                <IconX size={18} />
              </button>
            </div>

            <div style={{ padding: "var(--space-4) 0" }}>
              <p style={{ margin: "0 0 var(--space-3) 0", fontSize: "14px", color: "var(--color-text-1)" }}>
                ¿Deseas dar por terminada la consulta de <strong>{patientFullName}</strong>?
              </p>
              {finishError && <div className="banner danger" style={{ marginBottom: "var(--space-3)" }}>{finishError}</div>}

              <div style={{ backgroundColor: "var(--color-surface)", padding: "var(--space-3)", borderRadius: "6px", marginBottom: "var(--space-4)", border: "1px solid var(--color-border)" }}>
                <div style={{ fontSize: "13px", color: "var(--color-text-2)" }}>
                  ⏱️ Duración de consulta: <strong>{formatTimerSeconds(secondsElapsed)}</strong>
                </div>
                {plannedQuotationItem && (
                  <div style={{ fontSize: "13px", color: "var(--color-text-1)", marginTop: "4px" }}>
                    Procedimientos programados: <strong>{plannedQuotationItems.map((item) => item.description).join(" + ")}</strong>
                  </div>
                )}
                {appliedServices.length > 0 && (
                  <div style={{ fontSize: "13px", color: "var(--color-text-1)", marginTop: "4px" }}>
                    ⚕️ Servicios de catálogo aplicados: <strong>{appliedServices.length} (${totalAppliedServicesAmount})</strong>
                  </div>
                )}
                {effectiveMaterialUsage.length > 0 && (
                  <div style={{ fontSize: "13px", color: "var(--color-text-1)", marginTop: "4px" }}>
                    Insumos consumidos: <strong>{effectiveMaterialUsage.length} registrado(s)</strong>
                  </div>
                )}
              </div>

              {hasBillableActivity && (
                <label style={{ display: "flex", alignItems: "center", gap: "var(--space-2)", fontSize: "13px", fontWeight: 600, cursor: "pointer", color: "var(--color-primary)" }}>
                  <input
                    type="checkbox"
                    checked={createCashTicket}
                    onChange={(e) => setCreateCashTicket(e.target.checked)}
                  />
                  <IconReceiptTax size={18} /> Generar Orden de Cobro en Caja automáticamente
                </label>
              )}
            </div>

            <div className="form-actions">
              <button type="button" className="btn secondary" onClick={() => setFinishModalOpen(false)}>
                Cancelar
              </button>
              <button
                type="button"
                className="btn primary"
                onClick={handleConfirmFinishConsultation}
                disabled={completingConsultation}
                style={{ backgroundColor: "var(--color-success)", borderColor: "var(--color-success)" }}
              >
                {completingConsultation ? "Finalizando..." : "Finalizar y Cerrar Sesión"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
