# Plan de implementación — Módulo Personal · Nómina

Fecha: 2026-09-08
Estado: propuesta

## Contexto

La pestaña **Nómina** del módulo Personal es hoy un único scroll con tres paneles
apilados (`StaffPayrollPanel` en
[`clinerya-frontend/src/modules/staff/screens/PersonalScreen.tsx`](../clinerya-frontend/src/modules/staff/screens/PersonalScreen.tsx),
líneas 1078-1317):

1. **Nómina básica** → formulario "crear periodo" (nombre, inicio, fin), siempre visible.
2. **Selector de periodo** + badges de totales + "Cerrar periodo" + formulario suelto
   para capturar una línea (empleado, sueldo base, comisión, bono, deducción, notas).
3. **Tabla de líneas** + formulario de pago (cuenta operativa + "Pagar nómina").

El ciclo de vida real es `DRAFT → CLOSED → PAID` pero casi no se percibe en pantalla.

### Arquitectura relevante (hexagonal)

| Capa | Ubicación |
|---|---|
| Modelos de dominio | `clinerya-staff/clinerya-staff-domain/.../model/StaffPayrollPeriod.java`, `StaffPayrollLine.java`, `ClinicStaff.java` |
| Puerto de entrada | `clinerya-staff-domain/.../ports/in/ManageStaffOperationsUseCase.java` |
| Servicio de dominio | `clinerya-staff-domain/.../service/StaffOperationsService.java` |
| Puertos de salida | `clinerya-staff-domain/.../ports/out/StaffPayroll*RepositoryPort.java`, `PayrollAccountingPort.java` |
| Controller REST | `clinerya-staff/clinerya-staff-infra/.../adapters/in/web/StaffOperationsController.java` |
| Persistencia | `clinerya-staff-infra/.../adapters/out/Sql*Repository.java`, `SpringData*Repository.java`, `*Entity.java` |
| Cross-módulo contable | `clinerya-staff-infra/.../adapters/out/crossmodule/PayrollAccountingAdapter.java` |
| Migraciones Flyway | `clinerya-app/src/main/resources/db/migration/` — última `V40`, **siguiente libre: `V41`** |
| API frontend | `clinerya-frontend/src/shared/api/api.ts` (`staffApi`, ~línea 822) |
| Pantalla | `clinerya-frontend/src/modules/staff/screens/PersonalScreen.tsx` (`StaffPayrollPanel`) |

Tablas: `staff.staff_payroll_periods`, `staff.staff_payroll_lines`
(`V25__staff_operations.sql`), ampliadas en `V30__staff_payroll_payment.sql`.
Datos de actividad para comisiones: `staff.staff_activity_logs`
(`amount`, `occurred_at`, `type`, `staff_id`).

---

## Hallazgo bloqueante — resolver antes que nada

**`MANAGE_PAYROLL` está definido pero nunca se aplica.**
`StaffPermission.java:68` declara el permiso, pero `StaffOperationsController` no tiene
ninguna verificación de permisos (solo `ensureCurrentUserStaff` en asistencia).
Cualquier miembro del personal autenticado y con membresía de clínica puede
crear / cerrar / **pagar** nómina.

Es el mismo patrón que ya se corrigió en `ClinicStaffController`
(ver memoria "Staff privilege escalation", corregido 2026-09-05). Entra en la **P0**.

---

## Fases

### P0 — Seguridad + reestructura de UI

- **Esfuerzo:** M
- **Migración:** no
- **Riesgo:** bajo (frontend) / medio (el puerto de permisos toca firmas del dominio)

**Backend — HECHO (2026-09-08)**

- [x] Nuevo puerto de salida `StaffPermissionCheckerPort` +
  adaptador `StaffPermissionCheckerAdapter` que reutiliza
  `ManageClinicStaffUseCase.getPermissions` (roles + overrides).
- [x] `StaffOperationsService` recibe el puerto por constructor y aplica
  `requirePayrollPermission` (exige `MANAGE_PAYROLL`, rechaza `actingUserId` nulo →
  `ClinicAccessDeniedException` → HTTP 403) en `createPayrollPeriod`,
  `upsertPayrollLine`, `deletePayrollLine`, `closePayrollPeriod`,
  `payPayrollPeriod`.
- [x] `StaffOperationsController` resuelve `actingUserId` desde `Principal`
  (`currentUserId(...)`) y lo propaga; `ensureCurrentUserStaff` refactorizado para
  reutilizar el helper.
- [x] Wiring en `StaffDomainConfig`.
- [x] `StaffOperationsServiceTest` (5 casos) + `clinerya-staff` build OK +
  `clinerya-app` compila.

**Backend — pendiente (no bloqueante, mover a P5)**

- Confidencialidad de lectura: `listPayrollPeriods` / `listPayrollLines` siguen
  abiertas a cualquier miembro del personal. Gating por `MANAGE_PAYROLL` o
  "es su propia línea" requiere endurecer también `StaffDetailScreen`
  (hoy su `Promise.all` incluye `listPayrollPeriods` y un 403 rompería la
  pantalla) y `PersonalScreen.loadOperations` (trata un fallo de periodos como
  fatal en cualquier pestaña).

**Frontend — P0-B, alcance "gate + quick wins" HECHO (2026-09-08)**

En [`PersonalScreen.tsx`](../clinerya-frontend/src/modules/staff/screens/PersonalScreen.tsx):

- [x] `canManagePayroll = hasMyPermission("MANAGE_PAYROLL")`; la pestaña **Nómina**
  no se renderiza sin el permiso y, si el tab activo era `payroll`, se cae a
  `directory`. El render de `StaffPayrollPanel` también exige el permiso.
- [x] Presets de periodo ("Quincena actual" / "Mes actual" / "Semana actual") que
  rellenan `periodStart` / `periodEnd` y autogeneran el nombre
  (`payrollPeriodPreset(...)`), sin pisar un nombre ya escrito.
- [x] Fila de **totales** (`<tfoot>`) en la tabla de líneas: sueldo, comisión,
  bono, deducción y neto sumados, con el conteo de empleados.
- [x] Aviso inline de **neto estimado** bajo el formulario de línea; si la
  deducción supera al bruto se marca en rojo.
- [x] Tras pagar: se muestra el id de **póliza contable**
  (`paymentJournalEntryId`) con enlace a `/contabilidad/diario`.
- [x] `tsc -b --force` limpio.

Verificación en navegador con sesión iniciada: pendiente (requiere credenciales
del usuario).

**Frontend — reestructura de `StaffPayrollPanel` HECHA (2026-09-08)**

`PersonalScreen.tsx` pasó de 1620 → 1110 líneas; la nómina vive ahora en
archivos con una sola responsabilidad:

| Archivo | Función |
|---|---|
| `modules/staff/lib/payroll.ts` | Helpers puros y tipos: presets de periodo, `sumPayrollLines`, `estimateLineNet`, `payrollStage`, formato de moneda |
| `modules/staff/hooks/usePayroll.ts` | Todo el estado de nómina + llamadas API + acciones (crear, guardar línea con reconciliación, eliminar, cerrar, pagar) + anti-race. Desacopla la nómina de asistencia/actividad |
| `modules/staff/components/ConfirmDialog.tsx` | Modal de confirmación con resumen — reemplaza `window.confirm` en cerrar y pagar |
| `modules/staff/components/payroll/PayrollPanel.tsx` | Contenedor: sub-nav Periodos/Captura/Pago + stepper; consume `usePayroll` |
| `.../PayrollLifecycleStepper.tsx` | Capturar → Revisar → Cerrar → Pagar |
| `.../PayrollPeriodList.tsx` | Vista Periodos: solo la lista seleccionable + botón "Nuevo periodo" |
| `.../PayrollPeriodForm.tsx` | Página propia para crear un periodo (presets + fechas + nombre), con "Volver a periodos" |
| `.../PayrollCapture.tsx` | Vista Captura: cabecera del periodo + orquesta form y tabla + confirmar cierre |
| `.../PayrollLineForm.tsx` | Form de una línea con su propio estado (montos + preview de neto) |
| `.../PayrollLinesTable.tsx` | Tabla + `tfoot` de totales + acciones (confirmación al eliminar) |
| `.../PayrollPayment.tsx` | Vista Pago: resumen, cuenta, pagar con confirmación, póliza |

`PersonalScreen` solo renderiza `<PayrollPanel clinicId={clinicId} staff={staff} />`;
`loadOperations` ya no toca nómina ni cuentas bancarias. CSS del stepper/subnav/
totales añadido en `styles.css`. `tsc -b` + `vite build` limpios.

**Iteración 2 HECHA (2026-09-08)**

- [x] `PayrollLinesTable.tsx`: filtro por empleado (buscador) + orden por cualquier
  columna (clic en el encabezado, alterna asc/desc). El `tfoot` suma las filas
  visibles y muestra "Totales (N de M)" cuando hay filtro.
- [x] `CurrencyInput.tsx` (nuevo) + `PayrollLineForm.tsx`: los 4 montos son inputs
  de texto con separador de miles en reposo, número plano al enfocar; el form
  guarda `number | null` por campo.
- [x] `PayrollCapture.tsx`: badge "N de M capturados (X%)" en la cabecera del periodo.
- [x] CSS de la barra de filtro y encabezados ordenables en `styles.css`.
  `tsc -b` + `vite build` limpios.

**Pendiente**

- "% capturado / headcount **por periodo**" en la lista de Periodos (no solo el
  seleccionado): necesita un `lineCount` en `StaffPayrollPeriodResponse` desde el
  backend para evitar N+1 peticiones. Plegar en P1 o tarea backend pequeña.

---

### P1 — Compensación por empleado — HECHA (2026-09-08)

- **Migración:** `V41__staff_compensation.sql` — tabla nueva
  `staff.staff_compensation` (1-1 con `staff.clinic_staff`) en vez de columnas en
  `clinic_staff`, para no tocar archivos con WIP previo.
  Campos: `base_salary`, `pay_frequency`, `payment_method`,
  `payment_account_clabe`, `rfc`, `curp`, `nss`.

**Backend** (todo en archivos nuevos, cero cambios a `ClinicStaff*`)

- [x] Dominio: `StaffCompensation`, enums `StaffPayFrequency` / `StaffPaymentMethod`,
  `ManageStaffCompensationUseCase` (get / listByClinic / updateCompensation),
  `StaffCompensationRepositoryPort`, `StaffCompensationService` (exige
  `MANAGE_PAYROLL` vía el `StaffPermissionCheckerPort` de P0; valida que el
  empleado pertenece a la clínica; devuelve valores por defecto si no hay registro).
- [x] Infra: `StaffCompensationEntity`, `SpringData…` + `SqlStaffCompensationRepository`,
  `StaffCompensationController`
  (`GET /staff/compensation`, `GET|PUT /staff/{staffId}/compensation`),
  `StaffCompensationConfig` (clase de wiring propia).
- [x] `StaffCompensationServiceTest` (5 casos). Build `clinerya-staff` + `clinerya-app` OK.

**Frontend**

- [x] `api.ts`: `listStaffCompensation` / `getStaffCompensation` /
  `updateStaffCompensation` + tipos (los reads los usará P2).
- [~] Sub-vista **Configuracion** en `PayrollPanel` — **retirada** (decisión del
  usuario, 2026-09-08): la compensación se captura en el alta, no en una pestaña
  aparte. `PayrollCompensation.tsx` y `useStaffCompensation.ts` eliminados.

**Dependencias:** habilita P2 (generar líneas leyendo `base_salary`).

---

### P1b — Compensación en el alta de personal — HECHA (2026-09-08)

Decisión del usuario: el **admin captura todos los datos de nómina al invitar**;
los datos viven en la tabla 1-1 `staff_compensation`; sin sub-vista de
configuración. Permisos: `MANAGE_STAFF` para dar de alta + `MANAGE_PAYROLL` para
fijar el sueldo (los campos de nómina del form se muestran solo con ese permiso,
así se puede delegar RH concediendo ambos permisos).

Para **no re-entrelazar** con el WIP de `ClinicStaff*` / `UserController`:

- **Migración** `V42__staff_invitation_compensation.sql` — tabla puente que
  guarda la compensación entre invitar y confirmar.
- **Backend (archivos nuevos):** `StaffInvitationCompensation` +
  `StaffInvitationCompensationRepositoryPort`; `ManageStaffOnboardingUseCase` +
  `StaffOnboardingService` (envuelve `ManageClinicStaffUseCase` +
  `ManageStaffCompensationUseCase`; verifica `MANAGE_PAYROLL` antes de crear nada);
  infra: `StaffInvitationCompensationEntity` + repos, `StaffOnboardingController`
  (`POST /staff/onboarding/invitations`, `POST /staff/onboarding`),
  `StaffOnboardingConfig`.
- **`UserController.registerStaff`** (WIP file): **1 línea** —
  `staffOnboardingUseCase.applyInvitationCompensation(...)` tras crear el
  `ClinicStaff`, copia la compensación pendiente a `staff_compensation` y borra la
  puente. (1 field + 1 import + 1 línea.)
- **`StaffOnboardingServiceTest`** (5 casos). `mvn` staff = 23 tests verdes;
  `clinerya-app` compila.
- **Frontend:** `api.ts` `onboardInvite` + tipo; `AddStaffModal` con sección
  "Datos de nómina" (sueldo con `CurrencyInput`, periodicidad, método,
  CLABE/RFC/CURP/NSS) visible solo si `canManagePayroll`; apunta a
  `/staff/onboarding/invitations`. `tsc -b` + `vite build` limpios.

**Pendiente:** el form de "agregar usuario existente" (endpoint
`POST /staff/onboarding`) no tiene UI todavía — hoy `AddStaffModal` solo invita.

---

### P2 — Generar líneas del periodo — HECHA (2026-09-08)

**Backend** (todo en archivos ya propios: `StaffOperationsService` + su use case + controller)

- [x] `ManageStaffOperationsUseCase.generatePayrollLines(clinicId, actingUserId,
  periodId, PayrollLineSource)` con enum `BASE_COMPENSATION | PREVIOUS_PERIOD`.
- [x] `StaffOperationsService`: exige `MANAGE_PAYROLL`, periodo en `DRAFT`; **no
  sobrescribe** líneas ya capturadas.
  - `BASE_COMPENSATION`: por cada `ClinicStaff` activo sin línea → crea una con
    `baseSalary` = `staff_compensation` (0 si no hay), resto 0.
  - `PREVIOUS_PERIOD`: último periodo `CLOSED` (por `periodEnd`), copia
    base/comisión/bono/deducción/notas de cada línea cuyo empleado siga activo;
    error si no hay periodo cerrado.
  - Recalcula totales del periodo.
- [x] `POST /payroll/periods/{periodId}/lines/generate?source=base_compensation|previous_period`
  (`@Transactional`).
- [x] Nueva dependencia `StaffCompensationRepositoryPort` en `StaffOperationsService`
  (wiring en `StaffDomainConfig`). `StaffOperationsServiceTest` +4 casos
  (staff = 27 tests verdes); `clinerya-app` compila.

**Frontend**

- [x] `api.ts`: `generatePayrollLines` + tipo `PayrollLineSource`.
- [x] `usePayroll`: acción `generateLines(source)` (refresca líneas + totales).
- [x] `PayrollCapture`: barra con **"Generar desde sueldos base (N)"** y
  **"Copiar del periodo anterior"** (solo en borrador); N = faltantes por capturar.
- [x] `tsc -b` + `vite build` limpios.

---

### P3 — Comisiones automáticas desde actividad — v1 HECHA (2026-09-09)

Sin reglas ni tabla nueva: pass-through 100% de la actividad registrada.

**Backend** (en `StaffOperationsService`; sin dependencias ni config nuevas)

- [x] `previewPeriodCommissions(clinicId, actingUserId, periodId)` → por empleado
  con actividad en `[periodStart 00:00, periodEnd 23:59:59.999]`:
  `{ staffId, activityTotal, currentCommission }` (suma de
  `staff_activity_logs.amount` + la comisión que ya tiene su línea).
- [x] `applyPeriodCommissions(...)` → vuelca `activityTotal` en `commissionAmount`
  de la línea de cada empleado **con actividad y línea capturada** (no toca a los
  demás), recalcula bruto/neto y totales del periodo. Exige `DRAFT`.
- [x] `GET .../commission-preview` y
  `POST .../lines/apply-commissions` (`@Transactional`). Ambos exigen `MANAGE_PAYROLL`.
- [x] `StaffOperationsServiceTest` +3 (staff = 30 tests verdes); `clinerya-app` compila.

**Frontend**

- [x] `api.ts`: `previewPeriodCommissions` / `applyPeriodCommissions` + tipo
  `CommissionPreviewEntry`.
- [x] `usePayroll`: `commissionPreview`, `loadCommissionPreview`, `applyCommissions`
  (se limpia al cambiar de periodo).
- [x] `PayrollCommissionsPanel.tsx` dentro de **Captura** (solo borrador): botón
  "Ver comisiones" → tabla (empleado · actividad bruta · comisión actual) + total
  + "Aplicar a las líneas".
- [x] `tsc -b` + `vite build` limpios.

**Pendiente — v2 (reglas):** `V4x__commission_rules.sql`
(`clinic_id, staff_id NULL, activity_type NULL, percent`) y aplicar el `%` en vez
del pass-through.

---

### P4 — Desglose de conceptos + adelantos

- **Esfuerzo:** L (partible)
- **Migración:** `V4x`

**Migración**

- `staff.staff_payroll_line_items (line_id, kind PERCEPTION|DEDUCTION, concept, amount)`
- `staff.staff_advances (clinic_id, staff_id, amount, remaining, created_at)`

**Backend**

- `grossAmount` / `netAmount` de la línea pasan a calcularse desde los items
  (mantener `base/commission/bonus/deduction` como conveniencia o migrarlos a
  items "de sistema").
- Al generar líneas (P2), inyectar auto-deducción por saldo de adelantos y
  descontar `remaining` al pagar.

**Frontend**

- Fila expandible por empleado en la tabla de **Captura** con lista de
  percepciones / deducciones.
- Alta rápida de adelanto desde `StaffDetailScreen`.

**Nota:** partir en dos — primero items, luego adelantos.

---

### P5 — Recibos de nómina

- **Esfuerzo:** M/L
- **Dependencias:** mejor tras P4 (para mostrar el desglose real).

**Backend**

- `GET /payroll/periods/{id}/lines/{staffId}/receipt.pdf` (reusar el generador de
  PDF que ya use el proyecto).
- "Enviar por correo" vía el patrón `EmailSenderPort` existente.
- `GET .../receipts.zip` para todo el periodo.

**Frontend**

- Vista **Recibos**: descargar individual / todos / enviar.
- El empleado ve solo el suyo (gate por "es su línea").

---

### P6 — Pago por empleado + reversa + reportes

- **Esfuerzo:** L
- **Migración:** `V4x`

**Migración**

- `payment_status` + `paid_at` + `payment_account_id` + `payment_method` a nivel
  `staff_payroll_lines`.
- Permitir múltiples asientos contables por periodo.

**Backend**

- `pay` acepta pagos parciales / por cuenta / por empleado.
- Endpoint de reversa que anula el asiento y regresa el periodo a `CLOSED`.
- `PayrollAccountingAdapter` hoy asume una sola cuenta `DEBIT` → generalizar.

**Frontend**

- En **Pago**: método y cuenta por empleado, marcar pagado individual / masivo.
- En **Reportes**: export CSV/Excel para el contador, acumulado anual por empleado,
  costo de nómina por periodo vs. anterior.

**Extra:** paginar `listPayrollPeriods` (hoy devuelve todo).

---

## Secuencia recomendada

```
P0 (seguridad + UI)  →  P1 (compensación)  →  P2 (generar líneas)  →  P3 (comisiones)
                                                                        │
                                          P4 (conceptos/adelantos) ─────┤
                                                                        ├→ P5 (recibos)
                                                                        └→ P6 (pago granular + reportes)
```

- **P0 + P1 + P2** juntas cambian la experiencia por completo.
- **P3–P6** son incrementos independientes, priorizables según lo que pidan las clínicas.

---

## Propuesta de división de la UI

Convertir la pestaña **Nómina** en una sub-navegación con vistas enfocadas:

| Vista | Enfoque | Herramientas nuevas |
|---|---|---|
| **Periodos** | Lista con pill de estado (Borrador / Cerrada / Pagada), rango, headcount, Bruto/Neto, % capturado | "Nuevo periodo" en modal con presets de frecuencia · "Repetir último periodo" · plantillas |
| **Captura** (un periodo) | Editar líneas | Stepper de ciclo de vida · "Generar líneas" · "Copiar del anterior" · edición inline con fila de totales · filtro/búsqueda · "traer comisiones del rango" · validación inline · fila expandible para desglose + notas |
| **Pago** (periodo cerrado) | Ejecutar y registrar el pago | Resumen (headcount, neto, cuenta, fecha) · método y cuenta por empleado · marcar pagado individual/masivo · enlace al asiento en Contabilidad · ruta de reversa/ajuste |
| **Recibos** | Comprobantes | PDF por empleado · descargar todos · enviar por correo · historial y acumulado anual (el empleado ve solo el suyo) |
| **Configuración de nómina** | Datos maestros | Sueldo base, frecuencia, método, CLABE, datos fiscales · reglas de comisión · deducciones recurrentes y adelantos con saldo · conceptos personalizados |
| **Reportes** | Análisis | Costo por periodo (tendencia) · comparativo vs. anterior · acumulado anual · export CSV/Excel |

El permiso `MANAGE_PAYROLL` (ya existente) gatea cada sub-vista; cada empleado
ve solo sus propios recibos.
