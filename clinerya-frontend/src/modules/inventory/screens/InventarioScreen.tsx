import { useCallback, useEffect, useMemo, useState } from "react";
import type { Dispatch, FormEvent, SetStateAction } from "react";
import {
  IconAdjustments,
  IconArrowLeft,
  IconCircleCheck,
  IconCircleOff,
  IconClipboardList,
  IconEdit,
  IconPackage,
  IconPlus,
  IconRefresh,
  IconSearch,
  IconX
} from "@tabler/icons-react";
import {
  getFriendlyError,
  inventoryBatchesApi,
  inventoryLedgerApi,
  inventoryMovementsApi,
  inventoryReservationsApi,
  materialsApi
} from "@shared/api/api";
import {
  MOVEMENT_TYPE_LABELS,
  type BatchResponse,
  type GeneralLedgerEntryResponse,
  type InventoryMovementResponse,
  type MaterialReservationResponse,
  type MaterialResponse,
  type MovementType
} from "@modules/inventory/types";
import { InventoryPurchasesSection, InventorySuppliersSection } from "./InventoryPurchasingSections";

const currencyFormatter = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" });
const numberFormatter = new Intl.NumberFormat("es-MX", { maximumFractionDigits: 4 });
const appointmentDateFormatter = new Intl.DateTimeFormat("es-MX", {
  dateStyle: "medium",
  timeStyle: "short"
});

const ENTRY_TYPES: MovementType[] = ["PURCHASE_ENTRY", "ADJUSTMENT_IN"];
const BATCH_EXIT_TYPES: MovementType[] = ["ADJUSTMENT_OUT", "SALE_EXIT"];

const emptyMaterialForm = {
  name: "",
  category: "",
  internalCode: "",
  brand: "",
  description: "",
  unitOfMeasure: "pieza",
  presentationName: "",
  quantityPerPresentation: "",
  unitCost: "0",
  minimumStock: "",
  saleEnabled: false,
  salePrice: "",
  tracksBatches: false,
  active: true
};

const emptyMovementForm = {
  type: "PURCHASE_ENTRY" as MovementType,
  quantityMode: "BASE" as "BASE" | "PRESENTATION",
  quantity: "",
  presentationQuantity: "",
  movementDate: "",
  notes: "",
  lotNumber: "",
  expirationDate: "",
  batchId: ""
};

type MaterialFormState = typeof emptyMaterialForm;
type InventorySection = "materials" | "purchases" | "suppliers" | "movements" | "batches" | "reservations" | "alerts";
type BatchWithMaterial = BatchResponse & {
  materialName: string;
  materialUnitOfMeasure: string;
};

const inventoryTabs: Array<{ key: InventorySection; label: string }> = [
  { key: "materials", label: "Materiales" },
  { key: "purchases", label: "Compras" },
  { key: "suppliers", label: "Proveedores" },
  { key: "movements", label: "Movimientos" },
  { key: "batches", label: "Lotes y caducidades" },
  { key: "reservations", label: "Reservas" },
  { key: "alerts", label: "Alertas" }
];

const EXPIRING_SOON_DAYS = 30;
const STOCK_REFRESH_INTERVAL_MS = 8_000;

function formatPresentation(material: MaterialResponse) {
  if (!material.presentationName && !material.quantityPerPresentation) return "Sin presentación";
  const name = material.presentationName || "Presentación";
  if (!material.quantityPerPresentation) return name;
  return `${name} · ${numberFormatter.format(material.quantityPerPresentation)} ${material.unitOfMeasure}`;
}

function formatStockEquivalent(material: MaterialResponse) {
  if (!material.quantityPerPresentation || material.quantityPerPresentation <= 0) return "";
  const equivalent = material.currentStock / material.quantityPerPresentation;
  const label = material.presentationName || "presentaciones";
  return `${numberFormatter.format(equivalent)} ${label}`;
}

function formatMovementPresentation(movement: InventoryMovementResponse, material: MaterialResponse) {
  if (!movement.presentationQuantity || !movement.quantityPerPresentationAtMovement) return "";
  const label = movement.presentationNameAtMovement || "presentaciones";
  return `${numberFormatter.format(movement.presentationQuantity)} ${label} x ${numberFormatter.format(
    movement.quantityPerPresentationAtMovement
  )} ${material.unitOfMeasure}`;
}

function formatMaterialSubtext(material: MaterialResponse) {
  return [material.category, material.brand, material.internalCode].filter(Boolean).join(" · ");
}

function materialAvailableQuantity(material: MaterialResponse) {
  return material.availableQuantity ?? material.currentStock - (material.reservedQuantity ?? 0);
}

function isBatchExpiringSoon(batch: BatchResponse) {
  if (!batch.expirationDate || batch.depleted || batch.expired) return false;
  const expiration = new Date(`${batch.expirationDate}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const diffDays = Math.ceil((expiration.getTime() - today.getTime()) / 86_400_000);
  return diffDays >= 0 && diffDays <= EXPIRING_SOON_DAYS;
}

function formatRefreshTime() {
  return new Date().toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

export function InventarioScreen({ clinicId, hasClinic }: { clinicId?: string; hasClinic: boolean }) {
  const [materials, setMaterials] = useState<MaterialResponse[]>([]);
  const [movements, setMovements] = useState<InventoryMovementResponse[]>([]);
  const [batches, setBatches] = useState<BatchResponse[]>([]);
  const [selected, setSelected] = useState<MaterialResponse | null>(null);
  const [viewing, setViewing] = useState<MaterialResponse | null>(null);
  const [editing, setEditing] = useState<MaterialResponse | null>(null);
  const [materialForm, setMaterialForm] = useState<MaterialFormState>(emptyMaterialForm);
  const [movementForm, setMovementForm] = useState(emptyMovementForm);
  const [materialModalOpen, setMaterialModalOpen] = useState(false);
  const [activeSection, setActiveSection] = useState<InventorySection>("materials");
  const [ledgerEntries, setLedgerEntries] = useState<GeneralLedgerEntryResponse[]>([]);
  const [ledgerLoading, setLedgerLoading] = useState(false);
  const [allBatches, setAllBatches] = useState<BatchWithMaterial[]>([]);
  const [allBatchesLoading, setAllBatchesLoading] = useState(false);
  const [reservations, setReservations] = useState<MaterialReservationResponse[]>([]);
  const [reservationsLoading, setReservationsLoading] = useState(false);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [movementsLoading, setMovementsLoading] = useState(false);
  const [batchesLoading, setBatchesLoading] = useState(false);
  const [savingMaterial, setSavingMaterial] = useState(false);
  const [savingMovement, setSavingMovement] = useState(false);
  const [regularizingExpired, setRegularizingExpired] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [status, setStatus] = useState("");
  const [lastStockRefresh, setLastStockRefresh] = useState("");

  const loadMaterials = useCallback((options?: { silent?: boolean }) => {
    if (!clinicId) return;
    const silent = Boolean(options?.silent);
    if (!silent) {
      setLoading(true);
      setError("");
    }
    materialsApi
      .list(clinicId, true)
      .then((items) => {
        setMaterials(items);
        setLastStockRefresh(formatRefreshTime());
        setSelected((current) => {
          if (!current) return null;
          return items.find((item) => item.id === current.id) ?? null;
        });
      })
      .catch((caught) => {
        if (!silent) {
          setError(getFriendlyError(caught));
        }
      })
      .finally(() => {
        if (!silent) {
          setLoading(false);
        }
      });
  }, [clinicId]);

  const loadMovements = useCallback((materialId?: string, options?: { silent?: boolean }) => {
    if (!clinicId || !materialId) {
      setMovements([]);
      return;
    }
    const silent = Boolean(options?.silent);
    if (!silent) {
      setMovementsLoading(true);
    }
    inventoryMovementsApi
      .list(clinicId, materialId)
      .then(setMovements)
      .catch((caught) => {
        if (!silent) {
          setError(getFriendlyError(caught));
        }
      })
      .finally(() => {
        if (!silent) {
          setMovementsLoading(false);
        }
      });
  }, [clinicId]);

  const loadBatches = useCallback((materialId?: string, options?: { silent?: boolean }) => {
    if (!clinicId || !materialId) {
      setBatches([]);
      return;
    }
    const silent = Boolean(options?.silent);
    if (!silent) {
      setBatchesLoading(true);
    }
    inventoryBatchesApi
      .list(clinicId, materialId)
      .then(setBatches)
      .catch((caught) => {
        if (!silent) {
          setError(getFriendlyError(caught));
        }
      })
      .finally(() => {
        if (!silent) {
          setBatchesLoading(false);
        }
      });
  }, [clinicId]);

  const loadLedger = useCallback((options?: { silent?: boolean }) => {
    if (!clinicId) return;
    const silent = Boolean(options?.silent);
    if (!silent) {
      setLedgerLoading(true);
    }
    inventoryLedgerApi
      .list(clinicId)
      .then(setLedgerEntries)
      .catch((caught) => {
        if (!silent) {
          setError(getFriendlyError(caught));
        }
      })
      .finally(() => {
        if (!silent) {
          setLedgerLoading(false);
        }
      });
  }, [clinicId]);

  const loadAllBatches = useCallback((options?: { silent?: boolean }) => {
    if (!clinicId) return;
    const trackedMaterials = materials.filter((material) => material.tracksBatches);
    if (trackedMaterials.length === 0) {
      setAllBatches([]);
      return;
    }
    const silent = Boolean(options?.silent);
    if (!silent) {
      setAllBatchesLoading(true);
    }
    Promise.all(
      trackedMaterials.map((material) =>
        inventoryBatchesApi.list(clinicId, material.id).then((items) =>
          items.map((batch) => ({
            ...batch,
            materialName: material.name,
            materialUnitOfMeasure: material.unitOfMeasure
          }))
        )
      )
    )
      .then((groups) => setAllBatches(groups.flat()))
      .catch((caught) => {
        if (!silent) {
          setError(getFriendlyError(caught));
        }
      })
      .finally(() => {
        if (!silent) {
          setAllBatchesLoading(false);
        }
      });
  }, [clinicId, materials]);

  const regularizeExpiredBatches = async () => {
    if (!clinicId) return;
    setRegularizingExpired(true);
    setError("");
    setStatus("");
    try {
      const movements = await inventoryBatchesApi.regularizeExpired(clinicId);
      await loadMaterials({ silent: true });
      await loadAllBatches({ silent: true });
      if (selected?.id) {
        loadBatches(selected.id, { silent: true });
        loadMovements(selected.id, { silent: true });
      }
      setStatus(
        movements.length > 0
          ? `${movements.length} lote(s) caducado(s) registrados como merma.`
          : "No hay lotes caducados con existencia pendiente."
      );
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setRegularizingExpired(false);
    }
  };

  const loadReservations = useCallback((options?: { silent?: boolean }) => {
    if (!clinicId) {
      setReservations([]);
      return;
    }
    const silent = Boolean(options?.silent);
    if (!silent) {
      setReservationsLoading(true);
    }
    inventoryReservationsApi
      .listActive(clinicId)
      .then(setReservations)
      .catch((caught) => {
        if (!silent) {
          setError(getFriendlyError(caught));
        }
      })
      .finally(() => {
        if (!silent) {
          setReservationsLoading(false);
        }
      });
  }, [clinicId]);

  useEffect(() => {
    loadMaterials();
  }, [loadMaterials]);

  useEffect(() => {
    loadMovements(selected?.id);
    loadBatches(selected?.id);
  }, [selected?.id, loadMovements, loadBatches]);

  useEffect(() => {
    if (activeSection === "movements") {
      loadLedger();
    }
  }, [activeSection, loadLedger]);

  useEffect(() => {
    if (activeSection === "batches" || activeSection === "alerts") {
      loadAllBatches();
    }
  }, [activeSection, loadAllBatches]);

  useEffect(() => {
    if (activeSection === "reservations") {
      loadReservations();
    }
  }, [activeSection, loadReservations]);

  const refreshRealtimeStock = useCallback((options?: { silent?: boolean }) => {
    const silent = Boolean(options?.silent);
    loadMaterials({ silent });
    if (selected?.id) {
      loadMovements(selected.id, { silent });
      loadBatches(selected.id, { silent });
    }
    if (activeSection === "movements") {
      loadLedger({ silent });
    }
    if (activeSection === "reservations") {
      loadReservations({ silent });
    }
  }, [activeSection, loadBatches, loadLedger, loadMaterials, loadMovements, loadReservations, selected?.id]);

  useEffect(() => {
    if (!clinicId || !hasClinic) return;

    const refreshIfVisible = () => {
      if (document.visibilityState !== "visible" || savingMaterial || savingMovement) {
        return;
      }
      refreshRealtimeStock({ silent: true });
    };

    const intervalId = window.setInterval(refreshIfVisible, STOCK_REFRESH_INTERVAL_MS);
    window.addEventListener("focus", refreshIfVisible);
    document.addEventListener("visibilitychange", refreshIfVisible);

    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener("focus", refreshIfVisible);
      document.removeEventListener("visibilitychange", refreshIfVisible);
    };
  }, [clinicId, hasClinic, refreshRealtimeStock, savingMaterial, savingMovement]);

  useEffect(() => {
    if (!selected?.quantityPerPresentation && movementForm.quantityMode === "PRESENTATION") {
      setMovementForm((current) => ({ ...current, quantityMode: "BASE", presentationQuantity: "" }));
    }
  }, [selected?.quantityPerPresentation, movementForm.quantityMode]);

  const filteredMaterials = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return materials;
    return materials.filter((material) =>
      [material.name, material.unitOfMeasure, material.presentationName, material.category, material.brand, material.internalCode]
        .join(" ")
        .toLowerCase()
        .includes(term)
    );
  }, [materials, search]);

  const belowMinimumMaterials = useMemo(
    () => materials.filter((material) => material.belowMinimumStock),
    [materials]
  );

  const expiringBatches = useMemo(
    () => allBatches.filter((batch) => isBatchExpiringSoon(batch)),
    [allBatches]
  );

  const expiredBatches = useMemo(
    () => allBatches.filter((batch) => batch.expired && !batch.depleted),
    [allBatches]
  );

  const resetMaterialForm = () => {
    setEditing(null);
    setMaterialForm(emptyMaterialForm);
  };

  const openCreateMaterialModal = () => {
    resetMaterialForm();
    setMaterialModalOpen(true);
  };

  const closeMaterialModal = () => {
    setMaterialModalOpen(false);
    resetMaterialForm();
  };

  const startEdit = (material: MaterialResponse) => {
    setEditing(material);
    setMaterialForm({
      name: material.name,
      category: material.category || "",
      internalCode: material.internalCode || "",
      brand: material.brand || "",
      description: material.description || "",
      unitOfMeasure: material.unitOfMeasure,
      presentationName: material.presentationName || "",
      quantityPerPresentation: material.quantityPerPresentation ? String(material.quantityPerPresentation) : "",
      unitCost: String(material.unitCost),
      minimumStock: material.minimumStock ? String(material.minimumStock) : "",
      saleEnabled: material.saleEnabled,
      salePrice: material.salePrice ? String(material.salePrice) : "",
      tracksBatches: material.tracksBatches,
      active: material.active
    });
    setMaterialModalOpen(true);
  };

  const saveMaterial = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!clinicId) return;
    setSavingMaterial(true);
    setError("");
    setStatus("");

    try {
      const wasEditing = Boolean(editing);
      const payload = {
        name: materialForm.name.trim(),
        category: materialForm.category.trim() || undefined,
        internalCode: materialForm.internalCode.trim() || undefined,
        brand: materialForm.brand.trim() || undefined,
        description: materialForm.description.trim() || undefined,
        unitOfMeasure: materialForm.unitOfMeasure.trim(),
        presentationName: materialForm.presentationName.trim() || undefined,
        quantityPerPresentation: materialForm.quantityPerPresentation
          ? Number(materialForm.quantityPerPresentation)
          : undefined,
        unitCost: Number(materialForm.unitCost) || 0,
        minimumStock: materialForm.minimumStock ? Number(materialForm.minimumStock) : undefined,
        saleEnabled: materialForm.saleEnabled,
        salePrice: materialForm.saleEnabled ? Number(materialForm.salePrice) || 0 : undefined,
        tracksBatches: materialForm.tracksBatches
      };
      const saved = editing
        ? await materialsApi.update(clinicId, editing.id, { ...payload, active: materialForm.active })
        : await materialsApi.create(clinicId, payload);

      setMaterials((prev) => {
        const exists = prev.some((material) => material.id === saved.id);
        return exists ? prev.map((material) => (material.id === saved.id ? saved : material)) : [...prev, saved];
      });
      if (selected?.id === saved.id) {
        setSelected(saved);
      }
      setMaterialModalOpen(false);
      resetMaterialForm();
      setStatus(wasEditing ? "Material actualizado." : "Material creado. Registra una compra para cargar stock.");
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSavingMaterial(false);
    }
  };

  const toggleActive = async (material: MaterialResponse) => {
    if (!clinicId) return;
    setBusyId(material.id);
    setError("");
    setStatus("");
    try {
      if (material.active) {
        await materialsApi.remove(clinicId, material.id);
        const updated = { ...material, active: false };
        setMaterials((prev) => prev.map((item) => (item.id === material.id ? updated : item)));
        if (selected?.id === material.id) setSelected(updated);
        setStatus("Material desactivado.");
      } else {
        const updated = await materialsApi.update(clinicId, material.id, {
          name: material.name,
          category: material.category,
          internalCode: material.internalCode,
          brand: material.brand,
          description: material.description,
          unitOfMeasure: material.unitOfMeasure,
          presentationName: material.presentationName,
          quantityPerPresentation: material.quantityPerPresentation,
          unitCost: material.unitCost,
          minimumStock: material.minimumStock,
          saleEnabled: material.saleEnabled,
          salePrice: material.salePrice,
          tracksBatches: material.tracksBatches,
          active: true
        });
        setMaterials((prev) => prev.map((item) => (item.id === material.id ? updated : item)));
        if (selected?.id === material.id) setSelected(updated);
        setStatus("Material activado.");
      }
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusyId(null);
    }
  };

  const registerMovement = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!clinicId || !selected) return;
    setSavingMovement(true);
    setError("");
    setStatus("");
    try {
      const usePresentation =
        movementForm.quantityMode === "PRESENTATION" && Boolean(selected.quantityPerPresentation);
      const isEntry = ENTRY_TYPES.includes(movementForm.type);
      const isBatchExit = BATCH_EXIT_TYPES.includes(movementForm.type);
      await inventoryMovementsApi.register(clinicId, selected.id, {
        type: movementForm.type,
        quantity: usePresentation ? undefined : Number(movementForm.quantity) || 0,
        presentationQuantity: usePresentation ? Number(movementForm.presentationQuantity) || 0 : undefined,
        movementDate: movementForm.movementDate || undefined,
        notes: movementForm.notes.trim() || undefined,
        lotNumber: isEntry && selected.tracksBatches ? movementForm.lotNumber.trim() || undefined : undefined,
        expirationDate: isEntry && selected.tracksBatches ? movementForm.expirationDate || undefined : undefined,
        batchId: isBatchExit && selected.tracksBatches ? movementForm.batchId || undefined : undefined
      });
      const updated = await materialsApi.get(clinicId, selected.id);
      setMaterials((prev) => prev.map((material) => (material.id === updated.id ? updated : material)));
      setSelected(updated);
      setMovementForm(emptyMovementForm);
      loadMovements(updated.id);
      loadBatches(updated.id);
      if (activeSection === "batches" || activeSection === "alerts") {
        loadAllBatches({ silent: true });
      }
      setStatus("Movimiento registrado y stock actualizado.");
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSavingMovement(false);
    }
  };

  if (selected) {
    const isEntry = ENTRY_TYPES.includes(movementForm.type);
    const isBatchExit = BATCH_EXIT_TYPES.includes(movementForm.type);
    const availableBatches = batches.filter((batch) => !batch.depleted);

    return (
      <section className="dashboard-grid inventory-screen">
        <article className="panel full">
          <div className="panel-heading inventory-heading">
            <div className="inventory-heading-main">
              <button
                className="btn secondary"
                type="button"
                onClick={() => {
                  setSelected(null);
                  setMovements([]);
                  setBatches([]);
                  setMovementForm(emptyMovementForm);
                }}
              >
                <IconArrowLeft size={16} aria-hidden="true" />
                Inventario
              </button>
              <div>
                <h2>Kárdex</h2>
                <p className="panel-subtitle">
                  {selected.name} · {numberFormatter.format(selected.currentStock)} {selected.unitOfMeasure}
                  {" · disponible "}
                  {numberFormatter.format(materialAvailableQuantity(selected))} {selected.unitOfMeasure}
                  {" · reservado "}
                  {numberFormatter.format(selected.reservedQuantity ?? 0)} {selected.unitOfMeasure}
                  {selected.belowMinimumStock && <span className="badge warning">Bajo mínimo</span>}
                </p>
              </div>
            </div>
            <div className="panel-actions">
              <button className="btn ghost" type="button" onClick={() => startEdit(selected)}>
                <IconEdit size={16} aria-hidden="true" />
                Editar material
              </button>
            </div>
          </div>

          <form className="profile-form inventory-movement-form" onSubmit={registerMovement}>
            <label className="field">
              <span>Movimiento</span>
              <select
                value={movementForm.type}
                onChange={(event) =>
                  setMovementForm((current) => ({
                    ...current,
                    type: event.target.value as MovementType,
                    lotNumber: "",
                    expirationDate: "",
                    batchId: ""
                  }))
                }
              >
                <option value="PURCHASE_ENTRY">Compra</option>
                <option value="ADJUSTMENT_IN">Ajuste entrada</option>
                <option value="ADJUSTMENT_OUT">Ajuste salida</option>
                {selected.saleEnabled && <option value="SALE_EXIT">Venta directa</option>}
              </select>
            </label>
            <label className="field">
              <span>Capturar en</span>
              <select
                value={movementForm.quantityMode}
                onChange={(event) =>
                  setMovementForm((current) => ({
                    ...current,
                    quantityMode: event.target.value as "BASE" | "PRESENTATION",
                    quantity: "",
                    presentationQuantity: ""
                  }))
                }
              >
                <option value="BASE">Unidad base ({selected.unitOfMeasure})</option>
                {selected.quantityPerPresentation && (
                  <option value="PRESENTATION">{selected.presentationName || "Presentación"}</option>
                )}
              </select>
            </label>
            <label className="field">
              <span>
                {movementForm.quantityMode === "PRESENTATION" && selected.quantityPerPresentation
                  ? `Cantidad (${selected.presentationName || "presentaciones"})`
                  : `Cantidad (${selected.unitOfMeasure})`}
              </span>
              <input
                required
                min="0.0001"
                step="0.0001"
                type="number"
                value={
                  movementForm.quantityMode === "PRESENTATION" && selected.quantityPerPresentation
                    ? movementForm.presentationQuantity
                    : movementForm.quantity
                }
                onChange={(event) =>
                  setMovementForm((current) =>
                    current.quantityMode === "PRESENTATION" && selected.quantityPerPresentation
                      ? { ...current, presentationQuantity: event.target.value }
                      : { ...current, quantity: event.target.value }
                  )
                }
              />
            </label>
            <label className="field">
              <span>Fecha</span>
              <input
                type="datetime-local"
                value={movementForm.movementDate}
                onChange={(event) => setMovementForm((current) => ({ ...current, movementDate: event.target.value }))}
              />
            </label>
            {selected.tracksBatches && isEntry && (
              <>
                <label className="field">
                  <span>Número de lote (opcional)</span>
                  <input
                    value={movementForm.lotNumber}
                    onChange={(event) => setMovementForm((current) => ({ ...current, lotNumber: event.target.value }))}
                  />
                </label>
                <label className="field">
                  <span>Fecha de caducidad (opcional)</span>
                  <input
                    type="date"
                    value={movementForm.expirationDate}
                    onChange={(event) =>
                      setMovementForm((current) => ({ ...current, expirationDate: event.target.value }))
                    }
                  />
                </label>
              </>
            )}
            {selected.tracksBatches && isBatchExit && (
              <label className="field">
                <span>Lote a descontar</span>
                <select
                  value={movementForm.batchId}
                  onChange={(event) => setMovementForm((current) => ({ ...current, batchId: event.target.value }))}
                >
                  <option value="">Automático (primero en caducar)</option>
                  {availableBatches.map((batch) => (
                    <option key={batch.id} value={batch.id}>
                      {batch.lotNumber || "Sin folio"} · vence {batch.expirationDate || "s/f"} · disp.{" "}
                      {numberFormatter.format(batch.remainingQuantity)}
                    </option>
                  ))}
                </select>
              </label>
            )}
            <label className="field field-full">
              <span>Notas</span>
              <textarea
                value={movementForm.notes}
                onChange={(event) => setMovementForm((current) => ({ ...current, notes: event.target.value }))}
              />
            </label>
            <div className="form-actions">
              <button className="btn primary" type="submit" disabled={!selected.active || savingMovement}>
                <IconAdjustments size={16} aria-hidden="true" />
                {savingMovement ? "Registrando" : "Registrar movimiento"}
              </button>
            </div>
          </form>
        </article>

        {selected.tracksBatches && (
          <article className="panel full">
            <div className="panel-heading">
              <h2>Lotes y caducidad</h2>
              <span className="badge neutral">{batches.length}</span>
            </div>
            <div className="table-wrapper">
              <table className="data-table no-row-click">
                <thead>
                  <tr>
                    <th>Lote</th>
                    <th>Caducidad</th>
                    <th>Cantidad inicial</th>
                    <th>Restante</th>
                    <th>Costo</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {batchesLoading && (
                    <tr>
                      <td colSpan={6}>
                        <div className="empty-table-state">Cargando lotes...</div>
                      </td>
                    </tr>
                  )}
                  {!batchesLoading && batches.length === 0 && (
                    <tr>
                      <td colSpan={6}>
                        <div className="empty-table-state">
                          <strong>Sin lotes registrados</strong>
                          <span>Registra una compra con número de lote/caducidad para iniciar el control.</span>
                        </div>
                      </td>
                    </tr>
                  )}
                  {!batchesLoading &&
                    batches.map((batch) => (
                      <tr key={batch.id}>
                        <td>{batch.lotNumber || "Sin folio"}</td>
                        <td>{batch.expirationDate || "—"}</td>
                        <td>
                          {numberFormatter.format(batch.initialQuantity)} {selected.unitOfMeasure}
                        </td>
                        <td>
                          {numberFormatter.format(batch.remainingQuantity)} {selected.unitOfMeasure}
                        </td>
                        <td>{currencyFormatter.format(batch.unitCostAtEntry)}</td>
                        <td>
                          {batch.depleted ? (
                            <span className="badge neutral">Agotado</span>
                          ) : batch.expired ? (
                            <span className="badge warning">Caducado</span>
                          ) : (
                            <span className="badge success">Vigente</span>
                          )}
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </article>
        )}

        <article className="panel full">
          <div className="panel-heading">
            <h2>Historial de movimientos</h2>
            <span className="badge neutral">{movements.length}</span>
          </div>

          <div className="table-wrapper">
            <table className="data-table no-row-click">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Tipo</th>
                  <th>Cantidad</th>
                  <th>Costo</th>
                  <th>Notas</th>
                </tr>
              </thead>
              <tbody>
                {movementsLoading && (
                  <tr>
                    <td colSpan={5}>
                      <div className="empty-table-state">Cargando kárdex...</div>
                    </td>
                  </tr>
                )}
                {!movementsLoading && movements.length === 0 && (
                  <tr>
                    <td colSpan={5}>
                      <div className="empty-table-state">
                        <strong>Sin movimientos</strong>
                        <span>Registra una compra o ajuste para iniciar el kárdex.</span>
                      </div>
                    </td>
                  </tr>
                )}
                {!movementsLoading &&
                  movements.map((movement) => (
                    <tr key={movement.id}>
                      <td>{movement.movementDate.slice(0, 16).replace("T", " ")}</td>
                      <td>{MOVEMENT_TYPE_LABELS[movement.type]}</td>
                      <td>
                        <strong>
                          {numberFormatter.format(movement.quantity)} {selected.unitOfMeasure}
                        </strong>
                        {formatMovementPresentation(movement, selected) && (
                          <span className="table-subtext">{formatMovementPresentation(movement, selected)}</span>
                        )}
                      </td>
                      <td>{currencyFormatter.format(movement.unitCostAtMovement)}</td>
                      <td>{movement.notes || "—"}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </article>

        {materialModalOpen && (
          <div className="modal-overlay" onClick={closeMaterialModal}>
            <div className="modal-card" onClick={(event) => event.stopPropagation()}>
              <MaterialFormModalContent
                editing={editing}
                hasClinic={hasClinic}
                materialForm={materialForm}
                savingMaterial={savingMaterial}
                setMaterialForm={setMaterialForm}
                onClose={closeMaterialModal}
                onSubmit={saveMaterial}
              />
            </div>
          </div>
        )}

        {status && <p className="alert success">{status}</p>}
        {error && <p className="alert error">{error}</p>}
      </section>
    );
  }

  return (
    <section className="dashboard-grid inventory-screen">
      <section className="accounting-top-menu inventory-top-menu" aria-label="Menú de inventario">
        <div className="accounting-top-tabs" role="tablist" aria-label="Vistas de inventario">
          {inventoryTabs.map((item) => (
            <button
              key={item.key}
              type="button"
              className={activeSection === item.key ? "active" : ""}
              onClick={() => setActiveSection(item.key)}
            >
              {item.label}
            </button>
          ))}
        </div>

        <div
          className={
            activeSection === "materials"
              ? "accounting-top-actions inventory-top-actions-materials"
              : "accounting-top-actions accounting-top-actions-compact"
          }
        >
          {activeSection === "materials" && (
            <>
              <label className="accounting-search-field">
                <IconSearch size={16} aria-hidden="true" />
                <input
                  type="search"
                  placeholder="Buscar material, categoría, marca o código"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                />
              </label>
              <button className="btn secondary" type="button" disabled={!hasClinic || loading} onClick={() => loadMaterials()}>
                <IconRefresh size={16} aria-hidden="true" />
                Actualizar
              </button>
              <button className="btn primary" type="button" disabled={!hasClinic} onClick={openCreateMaterialModal}>
                <IconPlus size={16} aria-hidden="true" />
                Nuevo material
              </button>
            </>
          )}
          {activeSection === "movements" && (
            <button className="btn secondary" type="button" disabled={!hasClinic || ledgerLoading} onClick={() => loadLedger()}>
              <IconRefresh size={16} aria-hidden="true" />
              Actualizar movimientos
            </button>
          )}
          {(activeSection === "batches" || activeSection === "alerts") && (
            <>
              <button
                className="btn secondary"
                type="button"
                disabled={!hasClinic || allBatchesLoading || regularizingExpired}
                onClick={() => loadAllBatches()}
              >
                <IconRefresh size={16} aria-hidden="true" />
                Actualizar lotes
              </button>
              <button
                className="btn primary"
                type="button"
                disabled={!hasClinic || allBatchesLoading || regularizingExpired || expiredBatches.length === 0}
                onClick={regularizeExpiredBatches}
              >
                <IconClipboardList size={16} aria-hidden="true" />
                {regularizingExpired ? "Registrando..." : "Registrar mermas caducadas"}
              </button>
            </>
          )}
          {activeSection === "reservations" && (
            <button className="btn secondary" type="button" disabled={!hasClinic || reservationsLoading} onClick={() => loadReservations()}>
              <IconRefresh size={16} aria-hidden="true" />
              Actualizar reservas
            </button>
          )}
          {lastStockRefresh && (
            <span className="inventory-live-status">Stock en vivo · {lastStockRefresh}</span>
          )}
        </div>
      </section>

      {activeSection === "materials" && (
      <article className="panel full">
        <div className="panel-heading">
          <h2>Materiales de clínica</h2>
          <div className="panel-actions">
            <span className="badge neutral">{materials.length}</span>
          </div>
        </div>

        {!hasClinic && (
          <div className="clinic-list">
            <div className="clinic-row">
              <strong>Completa los datos de tu clínica</strong>
              <span>Necesitas una clínica activa para administrar inventario.</span>
            </div>
          </div>
        )}

        {hasClinic && (
          <>
            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Material</th>
                    <th>Presentación</th>
                    <th>Unidad</th>
                    <th>Costo unitario</th>
                    <th>Stock</th>
                    <th>Reservado</th>
                    <th>Disponible</th>
                    <th>Estado</th>
                    <th aria-label="Acciones" />
                  </tr>
                </thead>
                <tbody>
                  {loading && (
                    <tr>
                      <td colSpan={9}>
                        <div className="empty-table-state">Cargando inventario...</div>
                      </td>
                    </tr>
                  )}
                  {!loading &&
                    filteredMaterials.map((material) => (
                      <tr key={material.id} onClick={() => setViewing(material)}>
                        <td>
                          <strong>{material.name}</strong>
                          {formatMaterialSubtext(material) && (
                            <span className="table-subtext">{formatMaterialSubtext(material)}</span>
                          )}
                        </td>
                        <td>{formatPresentation(material)}</td>
                        <td>{material.unitOfMeasure}</td>
                        <td>{currencyFormatter.format(material.unitCost)}</td>
                        <td>
                          <strong>
                            {numberFormatter.format(material.currentStock)} {material.unitOfMeasure}
                          </strong>
                          {formatStockEquivalent(material) && (
                            <span className="table-subtext">{formatStockEquivalent(material)}</span>
                          )}
                          {material.belowMinimumStock && <span className="badge warning">Bajo mínimo</span>}
                        </td>
                        <td>
                          {numberFormatter.format(material.reservedQuantity ?? 0)} {material.unitOfMeasure}
                        </td>
                        <td>
                          <strong>
                            {numberFormatter.format(materialAvailableQuantity(material))} {material.unitOfMeasure}
                          </strong>
                        </td>
                        <td>
                          <span className={`badge ${material.active ? "success" : "neutral"}`}>
                            {material.active ? "Activo" : "Inactivo"}
                          </span>
                        </td>
                        <td className="table-actions">
                          <button
                            className="btn secondary"
                            type="button"
                            onClick={(event) => {
                              event.stopPropagation();
                              setSelected(material);
                              setMovements([]);
                              setBatches([]);
                              setMovementForm(emptyMovementForm);
                              setError("");
                              setStatus("");
                            }}
                          >
                            <IconClipboardList size={16} aria-hidden="true" />
                            Kárdex
                          </button>
                          <button
                            className="btn ghost"
                            type="button"
                            onClick={(event) => {
                              event.stopPropagation();
                              startEdit(material);
                            }}
                          >
                            <IconEdit size={16} aria-hidden="true" />
                            Editar
                          </button>
                          <button
                            className={material.active ? "btn destructive" : "btn ghost"}
                            type="button"
                            disabled={busyId === material.id}
                            onClick={(event) => {
                              event.stopPropagation();
                              toggleActive(material);
                            }}
                          >
                            {material.active ? (
                              <IconCircleOff size={16} aria-hidden="true" />
                            ) : (
                              <IconCircleCheck size={16} aria-hidden="true" />
                            )}
                            {busyId === material.id ? "..." : material.active ? "Desactivar" : "Activar"}
                          </button>
                        </td>
                      </tr>
                    ))}
                  {!loading && materials.length === 0 && (
                    <tr>
                      <td colSpan={9}>
                        <div className="empty-table-state">
                          <strong>Sin materiales registrados</strong>
                          <span>Crea el primer material y registra una compra para cargar stock.</span>
                        </div>
                      </td>
                    </tr>
                  )}
                  {!loading && materials.length > 0 && filteredMaterials.length === 0 && (
                    <tr>
                      <td colSpan={9}>
                        <div className="empty-table-state">Sin resultados para "{search}"</div>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </>
        )}
      </article>
      )}

      {activeSection === "purchases" && (
        <InventoryPurchasesSection
          clinicId={clinicId}
          hasClinic={hasClinic}
          materials={materials}
          onInventoryChanged={() => refreshRealtimeStock()}
        />
      )}

      {activeSection === "suppliers" && (
        <InventorySuppliersSection clinicId={clinicId} hasClinic={hasClinic} materials={materials} />
      )}

      {activeSection === "movements" && (
        <article className="panel full">
          <div className="panel-heading">
            <h2>Movimientos de inventario</h2>
            <span className="badge neutral">{ledgerEntries.length}</span>
          </div>
          <div className="table-wrapper">
            <table className="data-table no-row-click">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Material</th>
                  <th>Tipo</th>
                  <th>Cantidad</th>
                  <th>Costo</th>
                  <th>Referencia</th>
                  <th>Notas</th>
                </tr>
              </thead>
              <tbody>
                {ledgerLoading && (
                  <tr>
                    <td colSpan={7}>
                      <div className="empty-table-state">Cargando movimientos...</div>
                    </td>
                  </tr>
                )}
                {!ledgerLoading && ledgerEntries.length === 0 && (
                  <tr>
                    <td colSpan={7}>
                      <div className="empty-table-state">
                        <strong>Sin movimientos</strong>
                        <span>Aún no hay entradas, salidas o ajustes registrados.</span>
                      </div>
                    </td>
                  </tr>
                )}
                {!ledgerLoading &&
                  ledgerEntries.map((entry) => (
                    <tr key={entry.id}>
                      <td>{entry.movementDate.slice(0, 16).replace("T", " ")}</td>
                      <td>{entry.materialName}</td>
                      <td>{MOVEMENT_TYPE_LABELS[entry.type]}</td>
                      <td>
                        {numberFormatter.format(entry.quantity)} {entry.materialUnitOfMeasure}
                      </td>
                      <td>{currencyFormatter.format(entry.unitCostAtMovement)}</td>
                      <td>{entry.referenceType ? `${entry.referenceType} ${entry.referenceId ?? ""}` : "—"}</td>
                      <td>{entry.notes || "—"}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </article>
      )}

      {activeSection === "batches" && (
        <article className="panel full">
          <div className="panel-heading">
            <h2>Lotes y caducidades</h2>
            <span className="badge neutral">{allBatches.length}</span>
          </div>
          <div className="table-wrapper">
            <table className="data-table no-row-click">
              <thead>
                <tr>
                  <th>Material</th>
                  <th>Lote</th>
                  <th>Caducidad</th>
                  <th>Inicial</th>
                  <th>Restante</th>
                  <th>Costo</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {allBatchesLoading && (
                  <tr>
                    <td colSpan={7}>
                      <div className="empty-table-state">Cargando lotes...</div>
                    </td>
                  </tr>
                )}
                {!allBatchesLoading && allBatches.length === 0 && (
                  <tr>
                    <td colSpan={7}>
                      <div className="empty-table-state">
                        <strong>Sin lotes registrados</strong>
                        <span>Los materiales con control de lote aparecerán aquí después de una compra o ajuste de entrada.</span>
                      </div>
                    </td>
                  </tr>
                )}
                {!allBatchesLoading &&
                  allBatches.map((batch) => (
                    <tr key={batch.id}>
                      <td>{batch.materialName}</td>
                      <td>{batch.lotNumber || "Sin folio"}</td>
                      <td>{batch.expirationDate || "—"}</td>
                      <td>
                        {numberFormatter.format(batch.initialQuantity)} {batch.materialUnitOfMeasure}
                      </td>
                      <td>
                        {numberFormatter.format(batch.remainingQuantity)} {batch.materialUnitOfMeasure}
                      </td>
                      <td>{currencyFormatter.format(batch.unitCostAtEntry)}</td>
                      <td>
                        {batch.depleted ? (
                          <span className="badge neutral">Agotado</span>
                        ) : batch.expired ? (
                          <span className="badge warning">Caducado</span>
                        ) : isBatchExpiringSoon(batch) ? (
                          <span className="badge warning">Por caducar</span>
                        ) : (
                          <span className="badge success">Vigente</span>
                        )}
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </article>
      )}

      {activeSection === "reservations" && (
        <article className="panel full">
          <div className="panel-heading">
            <h2>Reservas de material</h2>
            <span className="badge neutral">{reservations.length}</span>
          </div>
          <div className="table-wrapper">
            <table className="data-table no-row-click">
              <thead>
                <tr>
                  <th>Paciente destino</th>
                  <th>Tratamiento</th>
                  <th>Material</th>
                  <th>Reservado</th>
                  <th>Disponible</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {reservationsLoading && (
                  <tr>
                    <td colSpan={6}>
                      <div className="empty-table-state">
                        <strong>Cargando reservas...</strong>
                      </div>
                    </td>
                  </tr>
                )}
                {!reservationsLoading && reservations.length === 0 && (
                  <tr>
                    <td colSpan={6}>
                      <div className="empty-table-state">
                        <strong>Sin reservas activas</strong>
                        <span>Las reservas aparecerán cuando Agenda aparte material para citas próximas.</span>
                      </div>
                    </td>
                  </tr>
                )}
                {!reservationsLoading && reservations.map((reservation) => (
                  <tr key={reservation.id}>
                    <td>
                      <strong>{reservation.patientName}</strong>
                      <span className="table-subtext">
                        Cita {appointmentDateFormatter.format(new Date(reservation.scheduledStart))}
                      </span>
                    </td>
                    <td>
                      <strong>{reservation.treatmentName}</strong>
                    </td>
                    <td>
                      <strong>{reservation.materialName}</strong>
                      <span className="table-subtext">
                        Stock: {numberFormatter.format(reservation.currentStock)} {reservation.unitOfMeasure}
                      </span>
                    </td>
                    <td>
                      {numberFormatter.format(reservation.quantity)} {reservation.unitOfMeasure}
                    </td>
                    <td>
                      <strong>
                        {numberFormatter.format(reservation.availableQuantity)} {reservation.unitOfMeasure}
                      </strong>
                    </td>
                    <td>
                      {reservation.availableQuantity < 0 ? (
                        <span className="badge warning">Revisar</span>
                      ) : (
                        <span className="badge success">Apartado</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </article>
      )}

      {activeSection === "alerts" && (
        <section className="dashboard-grid inventory-alert-grid">
          <article className="panel full">
            <div className="panel-heading">
              <h2>Alertas de inventario</h2>
              <span className="badge neutral">
                {belowMinimumMaterials.length + expiringBatches.length + expiredBatches.length}
              </span>
            </div>
            <div className="table-wrapper">
              <table className="data-table no-row-click">
                <thead>
                  <tr>
                    <th>Tipo</th>
                    <th>Elemento</th>
                    <th>Detalle</th>
                    <th>Prioridad</th>
                  </tr>
                </thead>
                <tbody>
                  {allBatchesLoading && (
                    <tr>
                      <td colSpan={4}>
                        <div className="empty-table-state">Cargando alertas...</div>
                      </td>
                    </tr>
                  )}
                  {!allBatchesLoading &&
                    belowMinimumMaterials.length === 0 &&
                    expiringBatches.length === 0 &&
                    expiredBatches.length === 0 && (
                      <tr>
                        <td colSpan={4}>
                          <div className="empty-table-state">
                            <strong>Sin alertas activas</strong>
                            <span>No hay materiales bajo mínimo ni lotes por caducar.</span>
                          </div>
                        </td>
                      </tr>
                    )}
                  {!allBatchesLoading &&
                    belowMinimumMaterials.map((material) => (
                      <tr key={`minimum-${material.id}`}>
                        <td>Stock mínimo</td>
                        <td>{material.name}</td>
                        <td>
                          Disponible {numberFormatter.format(materialAvailableQuantity(material))} {material.unitOfMeasure}
                          {material.minimumStock != null
                            ? ` · mínimo ${numberFormatter.format(material.minimumStock)} ${material.unitOfMeasure}`
                            : ""}
                        </td>
                        <td>
                          <span className="badge warning">Alta</span>
                        </td>
                      </tr>
                    ))}
                  {!allBatchesLoading &&
                    expiredBatches.map((batch) => (
                      <tr key={`expired-${batch.id}`}>
                        <td>Lote caducado</td>
                        <td>{batch.materialName}</td>
                        <td>
                          {batch.lotNumber || "Sin folio"} · restante {numberFormatter.format(batch.remainingQuantity)}{" "}
                          {batch.materialUnitOfMeasure}
                        </td>
                        <td>
                          <span className="badge warning">Alta</span>
                        </td>
                      </tr>
                    ))}
                  {!allBatchesLoading &&
                    expiringBatches.map((batch) => (
                      <tr key={`expiring-${batch.id}`}>
                        <td>Caducidad próxima</td>
                        <td>{batch.materialName}</td>
                        <td>
                          {batch.lotNumber || "Sin folio"} · vence {batch.expirationDate}
                        </td>
                        <td>
                          <span className="badge warning">Media</span>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </article>
        </section>
      )}

      {materialModalOpen && (
        <div className="modal-overlay" onClick={closeMaterialModal}>
          <div className="modal-card" onClick={(event) => event.stopPropagation()}>
            <MaterialFormModalContent
              editing={editing}
              hasClinic={hasClinic}
              materialForm={materialForm}
              savingMaterial={savingMaterial}
              setMaterialForm={setMaterialForm}
              onClose={closeMaterialModal}
              onSubmit={saveMaterial}
            />
          </div>
        </div>
      )}

      {viewing && (
        <div className="modal-overlay" onClick={() => setViewing(null)}>
          <div className="modal-card" onClick={(event) => event.stopPropagation()}>
            <MaterialDetailModalContent
              material={viewing}
              onClose={() => setViewing(null)}
              onEdit={() => {
                const material = viewing;
                setViewing(null);
                startEdit(material);
              }}
              onViewKardex={() => {
                const material = viewing;
                setViewing(null);
                setSelected(material);
                setMovements([]);
                setBatches([]);
                setMovementForm(emptyMovementForm);
                setError("");
                setStatus("");
              }}
            />
          </div>
        </div>
      )}

      {status && <p className="alert success">{status}</p>}
      {error && <p className="alert error">{error}</p>}
    </section>
  );
}

function MaterialFormModalContent({
  editing,
  hasClinic,
  materialForm,
  savingMaterial,
  setMaterialForm,
  onClose,
  onSubmit
}: {
  editing: MaterialResponse | null;
  hasClinic: boolean;
  materialForm: MaterialFormState;
  savingMaterial: boolean;
  setMaterialForm: Dispatch<SetStateAction<MaterialFormState>>;
  onClose: () => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}) {
  return (
    <>
      <div className="panel-heading">
        <h2>{editing ? "Editar material" : "Nuevo material"}</h2>
        <IconPackage size={20} aria-hidden="true" />
      </div>
      <form className="profile-form" onSubmit={onSubmit}>
        <label className="field field-full">
          <span>Nombre</span>
          <input
            required
            value={materialForm.name}
            onChange={(event) => setMaterialForm((current) => ({ ...current, name: event.target.value }))}
          />
        </label>
        <label className="field">
          <span>Categoría</span>
          <input
            placeholder="Anestésicos, Restauradores, Desechables..."
            value={materialForm.category}
            onChange={(event) => setMaterialForm((current) => ({ ...current, category: event.target.value }))}
          />
        </label>
        <label className="field">
          <span>Código interno</span>
          <input
            value={materialForm.internalCode}
            onChange={(event) => setMaterialForm((current) => ({ ...current, internalCode: event.target.value }))}
          />
        </label>
        <label className="field">
          <span>Marca</span>
          <input
            placeholder="3M, Dentsply, Ivoclar..."
            value={materialForm.brand}
            onChange={(event) => setMaterialForm((current) => ({ ...current, brand: event.target.value }))}
          />
        </label>
        <label className="field">
          <span>Unidad base</span>
          <input
            required
            placeholder="pieza, ml, g"
            value={materialForm.unitOfMeasure}
            onChange={(event) => setMaterialForm((current) => ({ ...current, unitOfMeasure: event.target.value }))}
          />
        </label>
        <label className="field">
          <span>Presentación</span>
          <input
            placeholder="Jeringa, frasco, caja"
            value={materialForm.presentationName}
            onChange={(event) => setMaterialForm((current) => ({ ...current, presentationName: event.target.value }))}
          />
        </label>
        <label className="field">
          <span>Contenido por presentación</span>
          <input
            min="0.0001"
            step="0.0001"
            type="number"
            value={materialForm.quantityPerPresentation}
            onChange={(event) =>
              setMaterialForm((current) => ({ ...current, quantityPerPresentation: event.target.value }))
            }
          />
        </label>
        <label className="field">
          <span>Costo unitario</span>
          <input
            required
            min="0"
            step="0.0001"
            type="number"
            value={materialForm.unitCost}
            onChange={(event) => setMaterialForm((current) => ({ ...current, unitCost: event.target.value }))}
          />
        </label>
        <label className="field">
          <span>Stock mínimo (opcional)</span>
          <input
            min="0"
            step="0.0001"
            type="number"
            value={materialForm.minimumStock}
            onChange={(event) => setMaterialForm((current) => ({ ...current, minimumStock: event.target.value }))}
          />
        </label>
        <label className="field field-full">
          <span>Descripción (opcional)</span>
          <textarea
            value={materialForm.description}
            onChange={(event) => setMaterialForm((current) => ({ ...current, description: event.target.value }))}
          />
        </label>
        <label className="field checkbox-field">
          <input
            type="checkbox"
            checked={materialForm.tracksBatches}
            onChange={(event) => setMaterialForm((current) => ({ ...current, tracksBatches: event.target.checked }))}
          />
          <span>Requiere control de lotes y caducidad</span>
        </label>
        <label className="field checkbox-field">
          <input
            type="checkbox"
            checked={materialForm.saleEnabled}
            onChange={(event) => setMaterialForm((current) => ({ ...current, saleEnabled: event.target.checked }))}
          />
          <span>Habilitado para venta directa al paciente</span>
        </label>
        {materialForm.saleEnabled && (
          <label className="field">
            <span>Precio de venta</span>
            <input
              required
              min="0"
              step="0.0001"
              type="number"
              value={materialForm.salePrice}
              onChange={(event) => setMaterialForm((current) => ({ ...current, salePrice: event.target.value }))}
            />
          </label>
        )}
        {editing && (
          <label className="field field-full">
            <span>Estado</span>
            <select
              value={materialForm.active ? "true" : "false"}
              onChange={(event) =>
                setMaterialForm((current) => ({ ...current, active: event.target.value === "true" }))
              }
            >
              <option value="true">Activo</option>
              <option value="false">Inactivo</option>
            </select>
          </label>
        )}
        <div className="form-actions">
          <button className="btn primary" type="submit" disabled={!hasClinic || savingMaterial}>
            <IconPackage size={16} aria-hidden="true" />
            {savingMaterial ? "Guardando" : "Guardar"}
          </button>
          <button className="btn ghost" type="button" onClick={onClose}>
            Cancelar
          </button>
        </div>
      </form>
    </>
  );
}

function MaterialDetailModalContent({
  material,
  onClose,
  onEdit,
  onViewKardex
}: {
  material: MaterialResponse;
  onClose: () => void;
  onEdit: () => void;
  onViewKardex: () => void;
}) {
  return (
    <>
      <div className="panel-heading">
        <h2>{material.name}</h2>
        <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
          <IconX size={18} />
        </button>
      </div>
      <div className="material-detail-grid">
        <div className="material-detail-item">
          <span>Categoría</span>
          <strong>{material.category || "—"}</strong>
        </div>
        <div className="material-detail-item">
          <span>Marca</span>
          <strong>{material.brand || "—"}</strong>
        </div>
        <div className="material-detail-item">
          <span>Código interno</span>
          <strong>{material.internalCode || "—"}</strong>
        </div>
        <div className="material-detail-item">
          <span>Estado</span>
          <strong>
            <span className={`badge ${material.active ? "success" : "neutral"}`}>
              {material.active ? "Activo" : "Inactivo"}
            </span>
          </strong>
        </div>
        <div className="material-detail-item">
          <span>Unidad base</span>
          <strong>{material.unitOfMeasure}</strong>
        </div>
        <div className="material-detail-item">
          <span>Presentación</span>
          <strong>{formatPresentation(material)}</strong>
        </div>
        <div className="material-detail-item">
          <span>Costo unitario</span>
          <strong>{currencyFormatter.format(material.unitCost)}</strong>
        </div>
        <div className="material-detail-item">
          <span>Stock actual</span>
          <strong>
            {numberFormatter.format(material.currentStock)} {material.unitOfMeasure}
            {material.belowMinimumStock && <span className="badge warning">Bajo mínimo</span>}
          </strong>
        </div>
        <div className="material-detail-item">
          <span>Reservado</span>
          <strong>
            {numberFormatter.format(material.reservedQuantity ?? 0)} {material.unitOfMeasure}
          </strong>
        </div>
        <div className="material-detail-item">
          <span>Disponible</span>
          <strong>
            {numberFormatter.format(materialAvailableQuantity(material))} {material.unitOfMeasure}
          </strong>
        </div>
        <div className="material-detail-item">
          <span>Stock mínimo</span>
          <strong>
            {material.minimumStock != null
              ? `${numberFormatter.format(material.minimumStock)} ${material.unitOfMeasure}`
              : "Sin definir"}
          </strong>
        </div>
        <div className="material-detail-item">
          <span>Control de lotes/caducidad</span>
          <strong>{material.tracksBatches ? "Sí" : "No"}</strong>
        </div>
        <div className="material-detail-item">
          <span>Venta directa</span>
          <strong>
            {material.saleEnabled ? `Sí · ${currencyFormatter.format(material.salePrice ?? 0)}` : "No habilitada"}
          </strong>
        </div>
        {material.description && (
          <div className="material-detail-item field-full">
            <span>Descripción</span>
            <strong>{material.description}</strong>
          </div>
        )}
      </div>
      <div className="form-actions">
        <button className="btn primary" type="button" onClick={onViewKardex}>
          <IconClipboardList size={16} aria-hidden="true" />
          Ver kárdex
        </button>
        <button className="btn ghost" type="button" onClick={onEdit}>
          <IconEdit size={16} aria-hidden="true" />
          Editar
        </button>
        <button className="btn ghost" type="button" onClick={onClose}>
          Cerrar
        </button>
      </div>
    </>
  );
}

function GeneralLedgerModalContent({
  entries,
  loading,
  onClose
}: {
  entries: GeneralLedgerEntryResponse[];
  loading: boolean;
  onClose: () => void;
}) {
  return (
    <>
      <div className="panel-heading">
        <h2>Kárdex general del inventario</h2>
        <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
          <IconX size={18} />
        </button>
      </div>
      <div className="table-wrapper scrollable-table-panel">
        <table className="data-table no-row-click">
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Material</th>
              <th>Tipo</th>
              <th>Cantidad</th>
              <th>Costo</th>
              <th>Notas</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan={6}>
                  <div className="empty-table-state">Cargando kárdex general...</div>
                </td>
              </tr>
            )}
            {!loading && entries.length === 0 && (
              <tr>
                <td colSpan={6}>
                  <div className="empty-table-state">
                    <strong>Sin movimientos</strong>
                    <span>Aún no hay movimientos registrados en el inventario de esta clínica.</span>
                  </div>
                </td>
              </tr>
            )}
            {!loading &&
              entries.map((entry) => (
                <tr key={entry.id}>
                  <td>{entry.movementDate.slice(0, 16).replace("T", " ")}</td>
                  <td>{entry.materialName}</td>
                  <td>{MOVEMENT_TYPE_LABELS[entry.type]}</td>
                  <td>
                    {numberFormatter.format(entry.quantity)} {entry.materialUnitOfMeasure}
                  </td>
                  <td>{currencyFormatter.format(entry.unitCostAtMovement)}</td>
                  <td>{entry.notes || "—"}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
      <div className="form-actions">
        <button className="btn ghost" type="button" onClick={onClose}>
          Cerrar
        </button>
      </div>
    </>
  );
}
