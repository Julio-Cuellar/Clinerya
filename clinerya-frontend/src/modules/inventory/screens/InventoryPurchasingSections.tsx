import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import {
  IconBan,
  IconBuildingStore,
  IconCheck,
  IconEdit,
  IconPackage,
  IconPlus,
  IconRefresh,
  IconSend,
  IconShoppingCart,
  IconTrash,
  IconTruckDelivery,
  IconX
} from "@tabler/icons-react";
import { getFriendlyError, inventorySuppliersApi, purchaseOrdersApi, accountingApi } from "@shared/api/api";
import {
  PURCHASE_ORDER_STATUS_LABELS,
  type MaterialResponse,
  type PurchaseOrderResponse,
  type PurchaseReceiptResponse,
  type SupplierMaterialResponse,
  type SupplierResponse
} from "@modules/inventory/types";
import type { BankAccountResponse } from "@modules/accounting/types";

const currencyFormatter = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" });
const numberFormatter = new Intl.NumberFormat("es-MX", { maximumFractionDigits: 4 });

const emptySupplierForm = {
  name: "",
  contactName: "",
  phone: "",
  email: "",
  taxId: "",
  notes: "",
  active: true
};

const emptyOrderLine = { materialId: "", quantity: "", unitCost: "" };

function todayInputValue() {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 10);
}

function statusBadgeClass(order: PurchaseOrderResponse) {
  if (order.status === "RECEIVED") return "success";
  if (order.status === "ORDERED" || order.status === "PARTIALLY_RECEIVED") return "warning";
  return "neutral";
}

function receivedProgress(order: PurchaseOrderResponse) {
  const ordered = order.lines.reduce((total, line) => total + line.orderedQuantity, 0);
  const received = order.lines.reduce((total, line) => total + line.receivedQuantity, 0);
  return ordered > 0 ? Math.round((received / ordered) * 100) : 0;
}

export function InventoryPurchasesSection({
  clinicId,
  hasClinic,
  materials,
  onInventoryChanged
}: {
  clinicId?: string;
  hasClinic: boolean;
  materials: MaterialResponse[];
  onInventoryChanged: () => void;
}) {
  const [orders, setOrders] = useState<PurchaseOrderResponse[]>([]);
  const [suppliers, setSuppliers] = useState<SupplierResponse[]>([]);
  const [supplierMaterials, setSupplierMaterials] = useState<SupplierMaterialResponse[]>([]);
  const [supplierMaterialsLoading, setSupplierMaterialsLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [status, setStatus] = useState("");
  const [orderModalOpen, setOrderModalOpen] = useState(false);
  const [receivingOrder, setReceivingOrder] = useState<PurchaseOrderResponse | null>(null);
  const [viewingOrder, setViewingOrder] = useState<PurchaseOrderResponse | null>(null);
  const [receipts, setReceipts] = useState<PurchaseReceiptResponse[]>([]);
  const [bankAccounts, setBankAccounts] = useState<BankAccountResponse[]>([]);
  const [orderForm, setOrderForm] = useState({
    supplierId: "",
    orderDate: todayInputValue(),
    expectedDate: "",
    notes: "",
    bankAccountId: "",
    lines: [{ ...emptyOrderLine }]
  });
  const [receiptForm, setReceiptForm] = useState({
    receivedAt: "",
    notes: "",
    lines: [] as Array<{
      purchaseOrderLineId: string;
      quantity: string;
      unitCost: string;
      lotNumber: string;
      expirationDate: string;
    }>
  });

  const activeMaterials = useMemo(() => materials.filter((material) => material.active), [materials]);
  const supplierMaterialIds = useMemo(
    () => new Set(supplierMaterials.map((item) => item.materialId)),
    [supplierMaterials]
  );
  const preferredMaterials = useMemo(
    () => activeMaterials.filter((material) => supplierMaterialIds.has(material.id)),
    [activeMaterials, supplierMaterialIds]
  );
  const otherMaterials = useMemo(
    () => activeMaterials.filter((material) => !supplierMaterialIds.has(material.id)),
    [activeMaterials, supplierMaterialIds]
  );

  const loadData = useCallback(async () => {
    if (!clinicId) return;
    setLoading(true);
    setError("");
    try {
      const [nextOrders, nextSuppliers, nextAccounts] = await Promise.all([
        purchaseOrdersApi.list(clinicId),
        inventorySuppliersApi.list(clinicId),
        accountingApi.listBankAccounts(clinicId).catch(() => [] as BankAccountResponse[])
      ]);
      setOrders(nextOrders);
      setSuppliers(nextSuppliers);
      setBankAccounts(nextAccounts.filter((acc) => acc.active));
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  }, [clinicId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const loadSupplierMaterials = useCallback(async (supplierId: string) => {
    if (!clinicId || !supplierId) {
      setSupplierMaterials([]);
      return;
    }
    setSupplierMaterialsLoading(true);
    try {
      setSupplierMaterials(await inventorySuppliersApi.listMaterials(clinicId, supplierId));
    } catch (caught) {
      setError(getFriendlyError(caught));
      setSupplierMaterials([]);
    } finally {
      setSupplierMaterialsLoading(false);
    }
  }, [clinicId]);

  function openOrderModal() {
    const supplierId = suppliers.find((supplier) => supplier.active)?.id ?? "";
    setOrderForm({
      supplierId,
      orderDate: todayInputValue(),
      expectedDate: "",
      notes: "",
      bankAccountId: "",
      lines: [{ ...emptyOrderLine }]
    });
    setError("");
    setStatus("");
    setOrderModalOpen(true);
    void loadSupplierMaterials(supplierId);
  }

  function changeOrderSupplier(supplierId: string) {
    setOrderForm((current) => ({ ...current, supplierId }));
    void loadSupplierMaterials(supplierId);
  }

  function updateOrderLine(index: number, field: "materialId" | "quantity" | "unitCost", value: string) {
    setOrderForm((current) => ({
      ...current,
      lines: current.lines.map((line, lineIndex) => {
        if (lineIndex !== index) return line;
        if (field === "materialId") {
          const material = activeMaterials.find((item) => item.id === value);
          const supplierMaterial = supplierMaterials.find((item) => item.materialId === value);
          return {
            ...line,
            materialId: value,
            unitCost: supplierMaterial
              ? String(supplierMaterial.supplierUnitCost)
              : material ? String(material.unitCost) : ""
          };
        }
        return { ...line, [field]: value };
      })
    }));
  }

  async function saveOrder(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!clinicId) return;
    setSaving(true);
    setError("");
    try {
      await purchaseOrdersApi.create(clinicId, {
        supplierId: orderForm.supplierId,
        orderDate: orderForm.orderDate,
        expectedDate: orderForm.expectedDate || undefined,
        notes: orderForm.notes || undefined,
        bankAccountId: orderForm.bankAccountId || undefined,
        lines: orderForm.lines.map((line) => ({
          materialId: line.materialId,
          quantity: Number(line.quantity),
          unitCost: Number(line.unitCost)
        }))
      });
      setOrderModalOpen(false);
      setStatus("Orden de compra creada como borrador.");
      await loadData();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  }

  async function transitionOrder(order: PurchaseOrderResponse, action: "ordered" | "cancel") {
    if (!clinicId) return;
    const confirmed = window.confirm(
      action === "ordered"
        ? `¿Enviar la orden ${order.folio} al proveedor?`
        : `¿Cancelar la orden ${order.folio}?`
    );
    if (!confirmed) return;
    setBusyId(order.id);
    setError("");
    try {
      if (action === "ordered") {
        await purchaseOrdersApi.markOrdered(clinicId, order.id);
        setStatus("La orden quedó lista para recepción.");
      } else {
        await purchaseOrdersApi.cancel(clinicId, order.id);
        setStatus("Orden cancelada.");
      }
      await loadData();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusyId(null);
    }
  }

  function openReceiptModal(order: PurchaseOrderResponse) {
    setReceiptForm({
      receivedAt: "",
      notes: "",
      lines: order.lines
        .filter((line) => line.remainingQuantity > 0)
        .map((line) => ({
          purchaseOrderLineId: line.id,
          quantity: "",
          unitCost: String(line.unitCost),
          lotNumber: "",
          expirationDate: ""
        }))
    });
    setError("");
    setStatus("");
    setReceivingOrder(order);
  }

  async function receiveOrder(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!clinicId || !receivingOrder) return;
    const lines = receiptForm.lines
      .filter((line) => Number(line.quantity) > 0)
      .map((line) => ({
        purchaseOrderLineId: line.purchaseOrderLineId,
        quantity: Number(line.quantity),
        unitCost: Number(line.unitCost),
        lotNumber: line.lotNumber || undefined,
        expirationDate: line.expirationDate || undefined
      }));
    if (lines.length === 0) {
      setError("Captura al menos una cantidad recibida.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      await purchaseOrdersApi.receive(clinicId, receivingOrder.id, {
        receivedAt: receiptForm.receivedAt || undefined,
        notes: receiptForm.notes || undefined,
        lines
      });
      setReceivingOrder(null);
      setStatus("Recepción registrada. El stock y los lotes ya fueron actualizados.");
      await loadData();
      onInventoryChanged();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  }

  async function viewOrder(order: PurchaseOrderResponse) {
    setViewingOrder(order);
    setReceipts([]);
    if (!clinicId) return;
    try {
      setReceipts(await purchaseOrdersApi.listReceipts(clinicId, order.id));
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  }

  return (
    <>
      <article className="panel full inventory-purchasing-panel">
        <div className="panel-heading">
          <div>
            <h2>Órdenes de compra</h2>
            <span className="table-subtext">Recepción de materiales y actualización automática del inventario</span>
          </div>
          <div className="panel-actions">
            <span className="badge neutral">{orders.length}</span>
            <button className="btn secondary" type="button" disabled={loading || !hasClinic} onClick={loadData}>
              <IconRefresh size={16} aria-hidden="true" />
              Actualizar
            </button>
            <button
              className="btn primary"
              type="button"
              disabled={!hasClinic || suppliers.every((supplier) => !supplier.active) || activeMaterials.length === 0}
              onClick={openOrderModal}
            >
              <IconPlus size={16} aria-hidden="true" />
              Nueva orden
            </button>
          </div>
        </div>

        {!loading && suppliers.every((supplier) => !supplier.active) && (
          <p className="alert warning">Registra un proveedor activo antes de crear una orden.</p>
        )}

        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Orden</th>
                <th>Proveedor</th>
                <th>Fechas</th>
                <th>Estado</th>
                <th>Recepción</th>
                <th>Total estimado</th>
                <th aria-label="Acciones" />
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr><td colSpan={7}><div className="empty-table-state">Cargando órdenes...</div></td></tr>
              )}
              {!loading && orders.length === 0 && (
                <tr>
                  <td colSpan={7}>
                    <div className="empty-table-state">
                      <strong>Sin órdenes de compra</strong>
                      <span>Crea una orden para solicitar y recibir materiales de un proveedor.</span>
                    </div>
                  </td>
                </tr>
              )}
              {!loading && orders.map((order) => (
                <tr key={order.id} onClick={() => viewOrder(order)}>
                  <td><strong>{order.folio}</strong><span className="table-subtext">{order.lines.length} partidas</span></td>
                  <td>{order.supplierName}</td>
                  <td>{order.orderDate}<span className="table-subtext">Entrega: {order.expectedDate || "Sin fecha"}</span></td>
                  <td><span className={`badge ${statusBadgeClass(order)}`}>{PURCHASE_ORDER_STATUS_LABELS[order.status]}</span></td>
                  <td>
                    <strong>{receivedProgress(order)}%</strong>
                    <div className="inventory-progress" aria-label={`${receivedProgress(order)}% recibido`}>
                      <span style={{ width: `${receivedProgress(order)}%` }} />
                    </div>
                  </td>
                  <td>{currencyFormatter.format(order.total)}</td>
                  <td className="table-actions" onClick={(event) => event.stopPropagation()}>
                    {order.status === "DRAFT" && (
                      <button className="btn secondary" type="button" disabled={busyId === order.id} onClick={() => transitionOrder(order, "ordered")}>
                        <IconSend size={16} aria-hidden="true" /> Enviar
                      </button>
                    )}
                    {(order.status === "ORDERED" || order.status === "PARTIALLY_RECEIVED") && (
                      <button className="btn primary" type="button" onClick={() => openReceiptModal(order)}>
                        <IconTruckDelivery size={16} aria-hidden="true" /> Recibir
                      </button>
                    )}
                    {(order.status === "DRAFT" || order.status === "ORDERED" || order.status === "PARTIALLY_RECEIVED") && (
                      <button className="icon-btn" type="button" aria-label="Cancelar orden" disabled={busyId === order.id} onClick={() => transitionOrder(order, "cancel")}>
                        <IconBan size={17} />
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </article>

      {orderModalOpen && (
        <div className="modal-overlay" onClick={() => setOrderModalOpen(false)}>
          <div className="modal-card inventory-purchase-modal" onClick={(event) => event.stopPropagation()}>
            <div className="panel-heading">
              <div><h2>Nueva orden de compra</h2><span className="table-subtext">El stock cambiará hasta registrar la recepción.</span></div>
              <button className="icon-btn" type="button" aria-label="Cerrar" onClick={() => setOrderModalOpen(false)}><IconX size={18} /></button>
            </div>
            <form className="profile-form" onSubmit={saveOrder}>
              <label className="field">
                <span>Proveedor</span>
                <select required value={orderForm.supplierId} onChange={(event) => changeOrderSupplier(event.target.value)}>
                  <option value="">Selecciona un proveedor</option>
                  {suppliers.filter((supplier) => supplier.active).map((supplier) => <option key={supplier.id} value={supplier.id}>{supplier.name}</option>)}
                </select>
                <small className="field-help">
                  {supplierMaterialsLoading
                    ? "Consultando materiales habituales..."
                    : `${supplierMaterials.length} materiales asociados a este proveedor`}
                </small>
              </label>
              <label className="field"><span>Fecha de orden</span><input required type="date" value={orderForm.orderDate} onChange={(event) => setOrderForm((current) => ({ ...current, orderDate: event.target.value }))} /></label>
              <label className="field"><span>Entrega estimada</span><input type="date" min={orderForm.orderDate} value={orderForm.expectedDate} onChange={(event) => setOrderForm((current) => ({ ...current, expectedDate: event.target.value }))} /></label>
              <label className="field">
                <span>Cuenta de Pago (Opcional)</span>
                <select value={orderForm.bankAccountId} onChange={(event) => setOrderForm((current) => ({ ...current, bankAccountId: event.target.value }))}>
                  <option value="">Ninguna (No genera póliza)</option>
                  {bankAccounts.map((account) => (
                    <option key={account.id} value={account.id}>
                      {account.alias || account.bankName} {account.accountLast4 ? `(****${account.accountLast4})` : ""}
                    </option>
                  ))}
                </select>
              </label>
              <label className="field field-full"><span>Notas</span><textarea value={orderForm.notes} onChange={(event) => setOrderForm((current) => ({ ...current, notes: event.target.value }))} /></label>

              <div className="field-full inventory-order-lines">
                <div className="inventory-order-lines-heading"><strong>Partidas</strong><button className="btn secondary" type="button" onClick={() => setOrderForm((current) => ({ ...current, lines: [...current.lines, { ...emptyOrderLine }] }))}><IconPlus size={16} /> Agregar material</button></div>
                {orderForm.lines.map((line, index) => (
                  <div className="inventory-order-line" key={index}>
                    <label className="field">
                      <span>Material</span>
                      <select required value={line.materialId} onChange={(event) => updateOrderLine(index, "materialId", event.target.value)}>
                        <option value="">Selecciona</option>
                        {preferredMaterials.length > 0 && (
                          <optgroup label="Materiales habituales del proveedor">
                            {preferredMaterials.map((material) => (
                              <option key={material.id} value={material.id}>{material.name} ({material.unitOfMeasure})</option>
                            ))}
                          </optgroup>
                        )}
                        {otherMaterials.length > 0 && (
                          <optgroup label={preferredMaterials.length > 0 ? "Otros materiales" : "Materiales de la clínica"}>
                            {otherMaterials.map((material) => (
                              <option key={material.id} value={material.id}>{material.name} ({material.unitOfMeasure})</option>
                            ))}
                          </optgroup>
                        )}
                      </select>
                    </label>
                    <label className="field"><span>Cantidad</span><input required min="0.0001" step="0.0001" type="number" value={line.quantity} onChange={(event) => updateOrderLine(index, "quantity", event.target.value)} /></label>
                    <label className="field"><span>Costo unitario</span><input required min="0.0001" step="0.0001" type="number" value={line.unitCost} onChange={(event) => updateOrderLine(index, "unitCost", event.target.value)} /></label>
                    <button className="icon-btn" type="button" aria-label="Quitar partida" disabled={orderForm.lines.length === 1} onClick={() => setOrderForm((current) => ({ ...current, lines: current.lines.filter((_, lineIndex) => lineIndex !== index) }))}><IconTrash size={17} /></button>
                  </div>
                ))}
              </div>
              <div className="inventory-order-total field-full"><span>Total estimado</span><strong>{currencyFormatter.format(orderForm.lines.reduce((total, line) => total + Number(line.quantity || 0) * Number(line.unitCost || 0), 0))}</strong></div>
              <div className="form-actions"><button className="btn primary" type="submit" disabled={saving}><IconShoppingCart size={16} /> {saving ? "Guardando" : "Crear orden"}</button><button className="btn ghost" type="button" onClick={() => setOrderModalOpen(false)}>Cancelar</button></div>
            </form>
          </div>
        </div>
      )}

      {receivingOrder && (
        <div className="modal-overlay" onClick={() => setReceivingOrder(null)}>
          <div className="modal-card inventory-purchase-modal" onClick={(event) => event.stopPropagation()}>
            <div className="panel-heading">
              <div><h2>Recibir {receivingOrder.folio}</h2><span className="table-subtext">Captura únicamente lo que llegó físicamente.</span></div>
              <button className="icon-btn" type="button" aria-label="Cerrar" onClick={() => setReceivingOrder(null)}><IconX size={18} /></button>
            </div>
            <form className="stack-form" onSubmit={receiveOrder}>
              <div className="inventory-receipt-lines">
                {receiptForm.lines.map((line, index) => {
                  const orderLine = receivingOrder.lines.find((item) => item.id === line.purchaseOrderLineId)!;
                  const material = materials.find((item) => item.id === orderLine.materialId);
                  return (
                    <div className="inventory-receipt-line" key={line.purchaseOrderLineId}>
                      <div className="inventory-receipt-line-heading"><strong>{orderLine.materialName}</strong><span>Pendiente: {numberFormatter.format(orderLine.remainingQuantity)} {orderLine.unitOfMeasure}</span></div>
                      <label className="field"><span>Cantidad recibida</span><input min="0" max={orderLine.remainingQuantity} step="0.0001" type="number" value={line.quantity} onChange={(event) => setReceiptForm((current) => ({ ...current, lines: current.lines.map((item, lineIndex) => lineIndex === index ? { ...item, quantity: event.target.value } : item) }))} /></label>
                      <label className="field"><span>Costo unitario real</span><input required min="0.0001" step="0.0001" type="number" value={line.unitCost} onChange={(event) => setReceiptForm((current) => ({ ...current, lines: current.lines.map((item, lineIndex) => lineIndex === index ? { ...item, unitCost: event.target.value } : item) }))} /></label>
                      {material?.tracksBatches && <><label className="field"><span>Lote</span><input required={Number(line.quantity) > 0} value={line.lotNumber} onChange={(event) => setReceiptForm((current) => ({ ...current, lines: current.lines.map((item, lineIndex) => lineIndex === index ? { ...item, lotNumber: event.target.value } : item) }))} /></label><label className="field"><span>Caducidad</span><input required={Number(line.quantity) > 0} type="date" value={line.expirationDate} onChange={(event) => setReceiptForm((current) => ({ ...current, lines: current.lines.map((item, lineIndex) => lineIndex === index ? { ...item, expirationDate: event.target.value } : item) }))} /></label></>}
                    </div>
                  );
                })}
              </div>
              <label className="field"><span>Fecha y hora de recepción (opcional)</span><input type="datetime-local" value={receiptForm.receivedAt} onChange={(event) => setReceiptForm((current) => ({ ...current, receivedAt: event.target.value }))} /></label>
              <label className="field"><span>Notas de recepción</span><textarea value={receiptForm.notes} onChange={(event) => setReceiptForm((current) => ({ ...current, notes: event.target.value }))} /></label>
              <div className="form-actions"><button className="btn primary" type="submit" disabled={saving}><IconTruckDelivery size={16} /> {saving ? "Registrando" : "Registrar recepción"}</button><button className="btn ghost" type="button" onClick={() => setReceivingOrder(null)}>Cancelar</button></div>
            </form>
          </div>
        </div>
      )}

      {viewingOrder && (
        <div className="modal-overlay" onClick={() => setViewingOrder(null)}>
          <div className="modal-card inventory-purchase-modal" onClick={(event) => event.stopPropagation()}>
            <div className="panel-heading"><div><h2>{viewingOrder.folio}</h2><span className="table-subtext">{viewingOrder.supplierName}</span></div><button className="icon-btn" type="button" aria-label="Cerrar" onClick={() => setViewingOrder(null)}><IconX size={18} /></button></div>
            <div className="inventory-order-summary"><span className={`badge ${statusBadgeClass(viewingOrder)}`}>{PURCHASE_ORDER_STATUS_LABELS[viewingOrder.status]}</span><strong>{currencyFormatter.format(viewingOrder.total)}</strong></div>
            <div className="table-wrapper"><table className="data-table no-row-click"><thead><tr><th>Material</th><th>Solicitado</th><th>Recibido</th><th>Pendiente</th><th>Costo</th></tr></thead><tbody>{viewingOrder.lines.map((line) => <tr key={line.id}><td>{line.materialName}</td><td>{numberFormatter.format(line.orderedQuantity)} {line.unitOfMeasure}</td><td>{numberFormatter.format(line.receivedQuantity)}</td><td>{numberFormatter.format(line.remainingQuantity)}</td><td>{currencyFormatter.format(line.unitCost)}</td></tr>)}</tbody></table></div>
            <div className="inventory-receipt-history"><h3>Recepciones</h3>{receipts.length === 0 ? <span className="table-subtext">Aún no se han registrado recepciones.</span> : receipts.map((receipt) => <div className="inventory-receipt-history-row" key={receipt.id}><span>{receipt.receivedAt.slice(0, 16).replace("T", " ")}</span><strong>{currencyFormatter.format(receipt.total)}</strong><span>{receipt.lines.length} partidas</span></div>)}</div>
            <div className="form-actions">{(viewingOrder.status === "ORDERED" || viewingOrder.status === "PARTIALLY_RECEIVED") && <button className="btn primary" type="button" onClick={() => { setViewingOrder(null); openReceiptModal(viewingOrder); }}><IconTruckDelivery size={16} /> Recibir materiales</button>}<button className="btn ghost" type="button" onClick={() => setViewingOrder(null)}>Cerrar</button></div>
          </div>
        </div>
      )}

      {status && <p className="alert success">{status}</p>}
      {error && <p className="alert error">{error}</p>}
    </>
  );
}

export function InventorySuppliersSection({
  clinicId,
  hasClinic,
  materials
}: {
  clinicId?: string;
  hasClinic: boolean;
  materials: MaterialResponse[];
}) {
  const [suppliers, setSuppliers] = useState<SupplierResponse[]>([]);
  const [editing, setEditing] = useState<SupplierResponse | null>(null);
  const [managingMaterials, setManagingMaterials] = useState<SupplierResponse | null>(null);
  const [supplierMaterials, setSupplierMaterials] = useState<SupplierMaterialResponse[]>([]);
  const [supplierMaterialForm, setSupplierMaterialForm] = useState({ materialId: "", supplierUnitCost: "" });
  const [form, setForm] = useState(emptySupplierForm);
  const [modalOpen, setModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [materialsLoading, setMaterialsLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [status, setStatus] = useState("");

  const associatedMaterialIds = useMemo(
    () => new Set(supplierMaterials.map((item) => item.materialId)),
    [supplierMaterials]
  );
  const availableMaterials = useMemo(
    () => materials.filter((material) => material.active && !associatedMaterialIds.has(material.id)),
    [associatedMaterialIds, materials]
  );

  const loadSuppliers = useCallback(async () => {
    if (!clinicId) return;
    setLoading(true);
    setError("");
    try {
      setSuppliers(await inventorySuppliersApi.list(clinicId));
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  }, [clinicId]);

  const loadSupplierMaterials = useCallback(async (supplierId: string) => {
    if (!clinicId) return;
    setMaterialsLoading(true);
    setError("");
    try {
      setSupplierMaterials(await inventorySuppliersApi.listMaterials(clinicId, supplierId));
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setMaterialsLoading(false);
    }
  }, [clinicId]);

  useEffect(() => { loadSuppliers(); }, [loadSuppliers]);

  function openSupplier(supplier?: SupplierResponse) {
    setEditing(supplier ?? null);
    setForm(supplier ? {
      name: supplier.name,
      contactName: supplier.contactName ?? "",
      phone: supplier.phone ?? "",
      email: supplier.email ?? "",
      taxId: supplier.taxId ?? "",
      notes: supplier.notes ?? "",
      active: supplier.active
    } : emptySupplierForm);
    setModalOpen(true);
    setError("");
    setStatus("");
  }

  function openSupplierMaterials(supplier: SupplierResponse) {
    setManagingMaterials(supplier);
    setSupplierMaterials([]);
    setSupplierMaterialForm({ materialId: "", supplierUnitCost: "" });
    setError("");
    setStatus("");
    void loadSupplierMaterials(supplier.id);
  }

  function changeSupplierMaterial(materialId: string) {
    const material = materials.find((item) => item.id === materialId);
    setSupplierMaterialForm({
      materialId,
      supplierUnitCost: material ? String(material.unitCost) : ""
    });
  }

  async function saveSupplier(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!clinicId) return;
    setSaving(true);
    setError("");
    try {
      if (editing) await inventorySuppliersApi.update(clinicId, editing.id, form);
      else await inventorySuppliersApi.create(clinicId, form);
      setModalOpen(false);
      setStatus(editing ? "Proveedor actualizado." : "Proveedor registrado.");
      await loadSuppliers();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  }

  async function addSupplierMaterial(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!clinicId || !managingMaterials) return;
    setSaving(true);
    setError("");
    try {
      await inventorySuppliersApi.addMaterial(clinicId, managingMaterials.id, {
        materialId: supplierMaterialForm.materialId,
        supplierUnitCost: Number(supplierMaterialForm.supplierUnitCost)
      });
      setSupplierMaterialForm({ materialId: "", supplierUnitCost: "" });
      await loadSupplierMaterials(managingMaterials.id);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  }

  async function removeSupplierMaterial(item: SupplierMaterialResponse) {
    if (!clinicId || !managingMaterials) return;
    if (!window.confirm(`¿Quitar ${item.materialName} de este proveedor?`)) return;
    setError("");
    try {
      await inventorySuppliersApi.removeMaterial(clinicId, managingMaterials.id, item.materialId);
      await loadSupplierMaterials(managingMaterials.id);
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  }

  async function toggleSupplier(supplier: SupplierResponse) {
    if (!clinicId) return;
    setError("");
    try {
      await inventorySuppliersApi.update(clinicId, supplier.id, {
        name: supplier.name,
        contactName: supplier.contactName,
        phone: supplier.phone,
        email: supplier.email,
        taxId: supplier.taxId,
        notes: supplier.notes,
        active: !supplier.active
      });
      await loadSuppliers();
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  }

  return (
    <>
      <article className="panel full inventory-purchasing-panel">
        <div className="panel-heading">
          <div><h2>Proveedores</h2><span className="table-subtext">Contactos y materiales que abastece cada proveedor</span></div>
          <div className="panel-actions">
            <span className="badge neutral">{suppliers.length}</span>
            <button className="btn secondary" type="button" disabled={loading || !hasClinic} onClick={loadSuppliers}><IconRefresh size={16} /> Actualizar</button>
            <button className="btn primary" type="button" disabled={!hasClinic} onClick={() => openSupplier()}><IconPlus size={16} /> Nuevo proveedor</button>
          </div>
        </div>
        <div className="table-wrapper">
          <table className="data-table no-row-click">
            <thead><tr><th>Proveedor</th><th>Contacto</th><th>Teléfono</th><th>Correo</th><th>RFC</th><th>Estado</th><th aria-label="Acciones" /></tr></thead>
            <tbody>
              {loading && <tr><td colSpan={7}><div className="empty-table-state">Cargando proveedores...</div></td></tr>}
              {!loading && suppliers.length === 0 && <tr><td colSpan={7}><div className="empty-table-state"><strong>Sin proveedores</strong><span>Registra el primer proveedor para comenzar a crear órdenes de compra.</span></div></td></tr>}
              {!loading && suppliers.map((supplier) => (
                <tr key={supplier.id}>
                  <td><strong>{supplier.name}</strong>{supplier.notes && <span className="table-subtext">{supplier.notes}</span>}</td>
                  <td>{supplier.contactName || "—"}</td>
                  <td>{supplier.phone || "—"}</td>
                  <td>{supplier.email || "—"}</td>
                  <td>{supplier.taxId || "—"}</td>
                  <td><span className={`badge ${supplier.active ? "success" : "neutral"}`}>{supplier.active ? "Activo" : "Inactivo"}</span></td>
                  <td className="table-actions">
                    <button className="btn secondary" type="button" onClick={() => openSupplierMaterials(supplier)}><IconPackage size={16} /> Materiales</button>
                    <button className="btn ghost" type="button" onClick={() => openSupplier(supplier)}><IconEdit size={16} /> Editar</button>
                    <button className="icon-btn" type="button" aria-label={supplier.active ? "Desactivar proveedor" : "Activar proveedor"} onClick={() => toggleSupplier(supplier)}>{supplier.active ? <IconBan size={17} /> : <IconCheck size={17} />}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </article>

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal-card" onClick={(event) => event.stopPropagation()}>
            <div className="panel-heading"><div><h2>{editing ? "Editar proveedor" : "Nuevo proveedor"}</h2><span className="table-subtext">Datos comerciales y de contacto</span></div><button className="icon-btn" type="button" aria-label="Cerrar" onClick={() => setModalOpen(false)}><IconX size={18} /></button></div>
            <form className="profile-form" onSubmit={saveSupplier}>
              <label className="field field-full"><span>Nombre comercial</span><input required value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} /></label>
              <label className="field"><span>Persona de contacto</span><input value={form.contactName} onChange={(event) => setForm((current) => ({ ...current, contactName: event.target.value }))} /></label>
              <label className="field"><span>Teléfono</span><input value={form.phone} onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))} /></label>
              <label className="field"><span>Correo</span><input type="email" value={form.email} onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))} /></label>
              <label className="field"><span>RFC</span><input value={form.taxId} onChange={(event) => setForm((current) => ({ ...current, taxId: event.target.value }))} /></label>
              <label className="field field-full"><span>Notas</span><textarea value={form.notes} onChange={(event) => setForm((current) => ({ ...current, notes: event.target.value }))} /></label>
              {editing && <label className="field"><span>Estado</span><select value={form.active ? "true" : "false"} onChange={(event) => setForm((current) => ({ ...current, active: event.target.value === "true" }))}><option value="true">Activo</option><option value="false">Inactivo</option></select></label>}
              <div className="form-actions"><button className="btn primary" type="submit" disabled={saving}><IconBuildingStore size={16} /> {saving ? "Guardando" : "Guardar proveedor"}</button><button className="btn ghost" type="button" onClick={() => setModalOpen(false)}>Cancelar</button></div>
            </form>
          </div>
        </div>
      )}

      {managingMaterials && (
        <div className="modal-overlay" onClick={() => setManagingMaterials(null)}>
          <div className="modal-card inventory-purchase-modal" onClick={(event) => event.stopPropagation()}>
            <div className="panel-heading">
              <div><h2>Materiales de {managingMaterials.name}</h2><span className="table-subtext">Catálogo habitual y costos conocidos del proveedor</span></div>
              <button className="icon-btn" type="button" aria-label="Cerrar" onClick={() => setManagingMaterials(null)}><IconX size={18} /></button>
            </div>
            <form className="inventory-supplier-material-add" onSubmit={addSupplierMaterial}>
              <label className="field">
                <span>Material</span>
                <select required value={supplierMaterialForm.materialId} onChange={(event) => changeSupplierMaterial(event.target.value)}>
                  <option value="">Selecciona un material</option>
                  {availableMaterials.map((material) => <option key={material.id} value={material.id}>{material.name} ({material.unitOfMeasure})</option>)}
                </select>
              </label>
              <label className="field"><span>Costo conocido</span><input required min="0.0001" step="0.0001" type="number" value={supplierMaterialForm.supplierUnitCost} onChange={(event) => setSupplierMaterialForm((current) => ({ ...current, supplierUnitCost: event.target.value }))} /></label>
              <button className="btn primary" type="submit" disabled={saving || availableMaterials.length === 0}><IconPlus size={16} /> Agregar</button>
            </form>
            <div className="table-wrapper">
              <table className="data-table no-row-click">
                <thead><tr><th>Material</th><th>Costo proveedor</th><th>Última recepción</th><th>Recepciones</th><th aria-label="Acciones" /></tr></thead>
                <tbody>
                  {materialsLoading && <tr><td colSpan={5}><div className="empty-table-state">Cargando materiales...</div></td></tr>}
                  {!materialsLoading && supplierMaterials.length === 0 && <tr><td colSpan={5}><div className="empty-table-state"><strong>Sin materiales asociados</strong><span>Agrega materiales o recíbelos desde una orden de compra.</span></div></td></tr>}
                  {!materialsLoading && supplierMaterials.map((item) => (
                    <tr key={item.id}>
                      <td><strong>{item.materialName}</strong><span className="table-subtext">{item.unitOfMeasure}</span></td>
                      <td>{currencyFormatter.format(item.supplierUnitCost)}</td>
                      <td>{item.lastSuppliedAt ? item.lastSuppliedAt.slice(0, 10) : "Asignado manualmente"}</td>
                      <td>{item.receiptCount}</td>
                      <td className="table-actions"><button className="btn ghost" type="button" onClick={() => removeSupplierMaterial(item)}><IconTrash size={16} /> Quitar</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {status && <p className="alert success">{status}</p>}
      {error && <p className="alert error">{error}</p>}
    </>
  );
}
