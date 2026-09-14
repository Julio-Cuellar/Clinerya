# Plan de implementación — Expediente clínico e historia clínica

Fecha: 2026-09-08
Estado: propuesta

## Contexto

El módulo **Expediente** (`clinerya-records` + `clinerya-frontend/src/modules/records`)
tiene una base sólida que **no llega a la pantalla**, y un submódulo —**recetas**—
que quedó fuera del modelo de seguridad del resto del expediente.

Lo que ya existe y funciona:

- **Historia clínica** por plantilla (`medical_histories`, un `answers_json` por
  par paciente+plantilla) con **versionado auditado** (`medical_history_versions`:
  usuario, nombre, IP, user-agent) y firma por campo
  (`MedicalHistorySignatureAuditService`).
- **Notas clínicas** SOAP + signos vitales, con **firma SHA-256** y registro en
  `document_signatures` (`ClinicalNote.sign(...)`, estado `DRAFT → SIGNED`
  irreversible).
- **Consentimiento de aviso de privacidad** (LFPDPPP) firmado por paciente.
- **Bitácora de accesos** (`record_access_logs`) con outbox
  (`SqlRecordAccessLogOutbox` + `RecordAccessLogOutboxDispatcher`), métricas y
  health indicator.
- **Compartir con especialista externo** con niveles `READ_ONLY / COMMENT / FULL`
  vía `clinerya-collaboration`.
- **Cifrado AES a nivel campo** (`AesCryptoConverter`).
- **Plantillas** NOM-004 y odontológica, con editor de canvas
  (`components/canvas/*`, `constants/nomHistoryTemplate.ts`,
  `constants/nomOdontologyTemplate.ts`) y **odontograma** (`OdontogramField`).
- **Export PDF** del expediente (`lib/pdfExport.ts`, ~1320 líneas, **client-side**).

### Arquitectura relevante (hexagonal)

| Capa | Ubicación |
|---|---|
| Modelos de dominio | `clinerya-records/clinerya-records-domain/.../model/` — `MedicalHistory.java`, `MedicalHistoryVersion.java`, `MedicalHistoryTemplate.java`, `ClinicalNote.java`, `VitalSigns.java` (record), `Prescription.java`, `PrescriptionItem.java`, `RecordAccessLog.java`, `PrivacyConsent.java`, `DocumentSignature.java`, `TemporaryRecordShare.java` |
| Puertos de entrada | `.../ports/in/` — `ManageMedicalHistoryUseCase`, `ManageClinicalNoteUseCase`, `ManagePrescriptionsUseCase`, `ManageHistoryTemplateUseCase`, `ManageRecordAccessLogUseCase`, `ManagePrivacyConsentUseCase`, `ManageTemporaryShareUseCase` |
| Servicios de dominio | `.../service/` — `MedicalHistoryService`, `ClinicalNoteService`, `PrescriptionService`, `HistoryTemplateService`, `RecordAccessLogService`, `PrivacyConsentService`, `TemporaryRecordShareService` |
| Puertos de salida | `.../ports/out/` — `*RepositoryPort`, `PatientAccessAuthorizationPort`, `PatientValidatorPort`, `PatientLookupPort`, `ClinicLookupPort`, `RecordAccessLogOutboxPort` |
| Controllers REST | `clinerya-records/clinerya-records-infra/.../adapters/in/web/` — `MedicalHistoryController`, `ClinicalNoteController`, `PrescriptionController`, `HistoryTemplateController`, `RecordAccessLogController`, `PrivacyConsentController`, `TemporaryRecordShareController` |
| Autorización cross-módulo | `.../adapters/out/crossmodule/RecordsAccessAuthorizationAdapter.java` (implementa `PatientAccessAuthorizationPort`, resuelve rol de staff + grants de colaboración) |
| Persistencia | `.../adapters/out/persistence/` — `Sql*Repository`, `SpringData*Repository`, `*Entity`, `*Mapper` |
| Config / wiring | `clinerya-records-infra/.../config/RecordsDomainConfig.java`, `TransactionalClinicalNoteUseCase.java` |
| Migraciones Flyway | `clinerya-app/src/main/resources/db/migration/` — `V43` (datos clínicos tipados) y `V44` (estado de revisión clínica) ya creadas por este plan; **siguiente libre: `V45`** |
| API frontend | `clinerya-frontend/src/shared/api/api.ts` — `medicalHistoryApi`, `clinicalNotesApi`, `prescriptionsApi` (~línea 339), `historyTemplatesApi`, `privacyConsentApi`, `attachmentsApi` |
| Pantalla | `clinerya-frontend/src/modules/records/screens/ExpedienteScreen.tsx` |
| Componentes | `clinerya-frontend/src/modules/records/components/` — `PatientHistoryPanel`, `ClinicalNotesSection`, `ClinicalNoteModal`, `HistoryFormModal`, `HistoryVersionsModal`, `PrescriptionSection`, `RecordAccessLogsModal`, `PrivacyConsentModal`, `TemplatesPanel`, `OdontogramField` |

`records-infra` ya depende de `staff-domain`, `patients-domain`,
`clinics-domain`, `collaboration-domain` y `users-infra`, así que
`PatientAccessAuthorizationPort`, `PatientValidatorPort` y
`ManageRecordAccessLogUseCase` son beans disponibles para inyectar en cualquier
servicio del módulo sin tocar el `pom`.

### Modelo de acceso vigente (`RecordsAccessAuthorizationAdapter`)

| Origen | Nivel |
|---|---|
| Staff `RECEPTIONIST` / `ACCOUNTANT` / `CLEANING` | `NONE` |
| Staff `ASSISTANT` | `READ_ONLY` |
| Staff resto de roles (`DOCTOR`, `CLINIC_ADMIN`, `ADMIN`, …) | `READ_WRITE` |
| Grant de colaboración `READ_ONLY` / `COMMENT` / `FULL` | mapea a `READ_ONLY` / `COMMENT` / `READ_WRITE` |

`ClinicalNoteService` y `MedicalHistoryService` llaman a `authorize(...)` al inicio
de **cada** método (lectura exige `!= NONE`, escritura exige `READ_WRITE`) y
registran en la bitácora vía el controller.

---

## Hallazgo bloqueante — resolver antes que nada

**Las recetas no tienen autorización ni bitácora.**
`ManagePrescriptionsUseCase` ni siquiera recibe `requestingUserId`.
`PrescriptionService` y `PrescriptionController` no llaman a
`PatientValidatorPort`, ni a `PatientAccessAuthorizationPort`, ni a
`ManageRecordAccessLogUseCase` — a diferencia de todos los demás servicios de
`records`.

Consecuencias:

- `GET /api/v1/clinics/{clinicId}/prescriptions/patient/{patientId}` — cualquier
  usuario autenticado lee las recetas de cualquier paciente de cualquier clínica.
  Es la **misma familia** del escalamiento de `ClinicStaffController` corregido el
  2026-09-05 (memoria "Staff privilege escalation").
- `issuePrescription` no valida que el paciente pertenezca a la clínica → se
  puede emitir una receta en la clínica A a nombre de un paciente de la clínica B.
- Emitir o leer una receta **no deja rastro** en `record_access_logs`. Para
  NOM-004 / LFPDPPP es justo lo que hay que auditar.
- Bug en `PrescriptionController.getPrescriptionById` (~línea 70):
  `managePrescriptionsUseCase.getPrescriptionsByPatient(clinicId, prescriptionId)`
  pasa un `prescriptionId` donde va un `patientId`. "Funciona" solo porque
  siempre devuelve vacío y cae al fallback.

Entra en la **P0**. ✅ Resuelto el 2026-09-08 (ver P0 abajo).

---

## Fases

### P0 — Autorización + bitácora en recetas — HECHO (2026-09-08)

- **Esfuerzo:** S (mecánico, replica un patrón existente)
- **Migración:** no
- **Riesgo:** bajo · **cambia firmas del puerto de entrada** `ManagePrescriptionsUseCase`

**Backend**

- [x] `ManagePrescriptionsUseCase`: `UUID requestingUserId` añadido a
  `issuePrescription`, `getPrescriptionsByPatient`, `getPrescriptionById`.
- [x] `PrescriptionService`: inyecta `PatientValidatorPort` +
  `PatientAccessAuthorizationPort` por constructor; método privado
  `authorize(requestingUserId, patientId, clinicId, requireWrite)` calcado de
  `MedicalHistoryService` (`NONE` → `ClinicAccessDeniedException` → 403; escritura
  exige `READ_WRITE`). Emitir = write; listar / obtener = read.
- [x] Bug de `getPrescriptionById` corregido: ahora
  `prescriptionRepository.findById(clinicId, prescriptionId)` y valida acceso
  sobre `prescription.getPatientId()`. El controller ya no hace el
  `getPrescriptionsByPatient(clinicId, prescriptionId)` + fallback.
- [x] `PrescriptionController`: `CurrentUserResolver` +
  `HttpServletRequest` (mismo patrón que `ClinicalNoteController`), `@Transactional`
  a nivel clase; propaga `currentUser.getId()` y llama
  `recordAccessLogUseCase.logAccess(..., "PRESCRIPTION", id, "WRITE"|"READ", ip, ua)`
  tras cada operación.
- [x] Wiring en `RecordsDomainConfig` (`prescriptionService(...)` gana dos deps).
- [x] `PrescriptionServiceTest` (8 casos, Mockito+AssertJ): sin acceso → 403,
  solo lectura no puede emitir, paciente de otra clínica → error, emisión OK con
  `READ_WRITE`, listar bloqueado sin acceso / permitido en `READ_ONLY`,
  `getById` autoriza contra el paciente de la receta, `getById` inexistente →
  error. `clinerya-records-infra` 23/23 verde · `clinerya-app` compila.

**Frontend**

- Sin cambios. `prescriptionsApi` (`listByPatient`, `issue`) usa rutas y cuerpos
  sin cambio; el back resuelve el usuario por token. Verificado: único llamador
  es `PrescriptionSection.tsx`.

---

### P1a — Cabecera clínica + datos críticos tipados — HECHO (2026-09-08)

- **Esfuerzo:** L
- **Migración:** `V43` (nuevas tablas en el schema `records`)
- **Riesgo:** medio (introduce un modelo tipado junto al `answers_json` libre)

**Problema.** No existe el concepto de **alergia**, **padecimiento crónico** ni
**medicación activa** en el dominio. Hoy "alergias" es una fila de texto libre
dentro del `answers_json` de una plantilla → **inconsultable** y sin mostrarse en
ningún lado al recetar, aunque el dato esté capturado. `Patient.bloodType` existe
y no se muestra.

**Decisión de alcance (regulatoria).** Nada en esta fase evalúa ni cruza datos
clínicos: no hay motor de alertas, no se compara el fármaco recetado contra las
alergias, no se calcula severidad ni se bloquea nada. Todo es **display pasivo**
de lo que el clínico capturó — mismo estatus de "software de expediente" que ya
se tiene por guardar notas. Un cruce `fármaco ↔ alergia` o cualquier aviso
automático entraría en soporte a la decisión clínica y queda **fuera** del plan.

**Migración `V43__patient_clinical_data.sql`** — [x] creada, tablas en schema `records`:

- `patient_allergies (id, clinic_id, patient_id, substance, reaction, severity
  [MILD|MODERATE|SEVERE|UNKNOWN], category [DRUG|FOOD|ENVIRONMENTAL|OTHER], source
  [MANUAL|TEMPLATE], noted_by_user_id, noted_by_user_name, noted_at)`.
  `category` (añadido sobre el plan) permite que el recuadro de Recetas filtre a
  alergias a fármacos.
- `patient_conditions (id, clinic_id, patient_id, name, icd10_code NULL, status
  [ACTIVE|RESOLVED], onset_date NULL, source, noted_by_*, noted_at)`.
- `patient_medications (id, clinic_id, patient_id, medication_name, dose, schedule,
  active BOOLEAN, started_on NULL, stopped_on NULL, prescription_id NULL, source,
  noted_by_*, noted_at)`.
- Índices `(clinic_id, patient_id)` en las tres.

**Backend** — HECHO

- [x] Modelos `PatientAllergy` / `PatientCondition` / `PatientMedication` + enums
  `AllergySeverity` / `AllergyCategory` / `ConditionStatus` / `ClinicalDataSource`.
- [x] `PatientAllergyRepositoryPort` / `PatientConditionRepositoryPort` /
  `PatientMedicationRepositoryPort` (`save`, `findByClinicIdAndPatientId`,
  `findByIdAndClinicId`, `deleteByIdAndClinicId`) + `*Entity` + `SpringData*` +
  `Sql*` con mapeo inline (sin interfaz mapper aparte).
- [x] `ManagePatientClinicalSummaryUseCase`:
  `getSummary(...)` → `ClinicalSummary { patientId, bloodType, allergies[],
  conditions[], activeMedications[], lastNoteAt }` + CRUD de las 3 entidades con
  `AllergyInput` / `ConditionInput` / `MedicationInput`.
  `lastVisitAt` se pospone (necesita cross-módulo con agenda); `lastNoteAt` sale
  de `ClinicalNoteRepositoryPort`.
- [x] `PatientClinicalSummaryService` en `records-domain`: `authorize(...)` calcado
  de `MedicalHistoryService` (lectura `!= NONE`, escritura exige `READ_WRITE`);
  medicación se filtra a `active` en el resumen; add/update sellan
  `notedByUserId/Name/At` y `source = MANUAL`.
- [x] `PatientLookupPort.PatientDetails` extendido con `bloodType` (+ adapter).
- [x] **Estado "sin ... reportados / preguntadas y negadas"** por tipo de dato
  (`V44__patient_clinical_reviews.sql`): tabla `records.patient_clinical_reviews
  (id, clinic_id, patient_id, kind [ALLERGIES|CONDITIONS|MEDICATIONS],
  none_reported, reviewed_by_*, reviewed_at)` con `UNIQUE (clinic_id, patient_id,
  kind)`. Modelo `PatientClinicalReview` + enum `ClinicalReviewKind` +
  `PatientClinicalReviewRepositoryPort` + `Entity`/`SpringData`/`Sql`.
  `ClinicalSummary` gana `allergiesReview` / `conditionsReview` /
  `medicationsReview` (record `ReviewStatus { noneReported, reviewedByUserName,
  reviewedAt }`); `setClinicalReview(..., kind, noneReported)` hace upsert por
  `kind` (exige `READ_WRITE`).
- [x] `PatientClinicalSummaryController` `@Transactional`:
  `GET /api/v1/patients/{patientId}/clinical-summary?clinicId=` +
  `PUT .../{allergies|conditions|medications}/review` +
  `POST|PUT|DELETE .../allergies|conditions|medications[/{id}]`, `clinicId` por
  query, cada operación llama `recordAccessLogUseCase.logAccess(...,
  "CLINICAL_SUMMARY"|"PATIENT_ALLERGY"|"PATIENT_CLINICAL_REVIEW"|"PATIENT_CONDITION"
  |"PATIENT_MEDICATION", id, "READ"|"WRITE", ip, ua)`. DTO
  `PatientClinicalSummaryResponse` (con `ReviewDto` × 3).
- [x] Wiring en `RecordsDomainConfig` (bean `patientClinicalSummaryService`; los
  `Sql*Repository` son `@Repository` component-scan).
- [x] `PatientClinicalSummaryServiceTest` (10 casos, incl. review por `kind`).
  `clinerya-records-infra` 33/33 verde · `clinerya-app` compila.

**Backend — pendiente (ver P1c)**

- Sincronización desde plantilla → **P1c** (abajo).

**Frontend** — HECHO (2026-09-08)

- [x] Tipos en `modules/records/types.ts` (`PatientClinicalSummaryResponse` con
  `allergiesReview` / `conditionsReview` / `medicationsReview: ClinicalReviewDto`,
  `PatientAllergyDto` / `ConditionDto` / `MedicationDto`, inputs, mapas de labels)
  + `clinicalSummaryApi` en `api.ts` (`get`, `setReview(kind, noneReported)`, CRUD
  de las 3 entidades, `clinicId` por query).
- [x] `PatientClinicalHeader` — banda **alergias · crónicos · medicación activa ·
  tipo de sangre · última consulta**, celdas etiqueta-sobre-valor, `grid-column:
  1 / -1` (ocupa el ancho del `dashboard-grid`). Cada celda: lista si hay datos
  (alergias en rojo), o *"Preguntadas y negadas"* / *"Sin padecimientos
  reportados"* / *"Sin medicación activa"* si el flag está puesto, o *"Sin
  registrar"*. Montada en `PatientRecord` (`ExpedienteScreen`) y en
  `PatientHistoryPanel` con prop `showClinicalHeader` (default `true`; el
  expediente la pasa `false`). `canEdit = !historyReadOnly`.
- [x] `ClinicalDataEditor` — modal (`Gestionar`) con **alta y baja** de alergias,
  padecimientos y medicación. **Cada cambio (alta, baja, casilla) confirma primero
  los datos capturados** en un `ConfirmDialog` con estilos (no `window.confirm`),
  que reproduce el dato; borrar usa `tone="danger"`. Casilla por sección:
  *"Preguntadas y negadas"* / *"Sin padecimientos reportados"* / *"Sin medicación
  activa"* (deshabilitada si esa sección ya tiene datos) → `setReview`. Formularios
  con etiquetas; medicación con **fecha de inicio y fin** (`startedOn` /
  `stoppedOn`); padecimientos con `onsetDate`. Edición en sitio = follow-up menor.
- [x] `DrugAllergyBox` — recuadro pasivo en `PrescriptionSection`: solo lectura,
  filtra `category === "DRUG"`, encabezado con procedencia
  *"actualizado {fecha} por {autor}"*, vacío → *"Sin alergias a fármacos
  registradas"* o *"Preguntadas y negadas: sin alergias a fármacos conocidas"* si
  `allergiesReview.noneReported`. Sin coincidencia con el fármaco recetado, sin
  severidad calculada, sin aviso.
- [ ] Reutilizar `DrugAllergyBox` en `ClinicalNoteModal` — pendiente (menor).
- [x] `tsc -b` + `vite build` limpios.

---

### P1b — Pestaña Recetas + cronología unificada — HECHO (2026-09-08)

- **Esfuerzo:** M
- **Migración:** no
- **Riesgo:** bajo

**Problema.** `PrescriptionSection` solo se monta en `PatientCareScreen`
(tratamientos). Desde el expediente no se ve qué le recetaron al paciente. Y las
5 pestañas (`historia / tratamientos / citas / pagos / estudios`) son 5 fetches
independientes en silos, sin vista cronológica.

**Backend — HECHO**

- [x] `GET /api/v1/clinics/{clinicId}/appointments/by-patient/{patientId}` en
  `clinerya-agenda` (`AppointmentController` → `ManageAppointmentsUseCase.listByPatient`
  → `AppointmentRepositoryPort.findByPatientIdAndClinicId` →
  `findByPatientIdAndClinicIdOrderByScheduledStartDesc`), mismo patrón que
  `by-quotation`. `agendaApi.listByPatient(clinicId, patientId)` en el frontend.
  Elimina el fetch de ±2 años de toda la clínica filtrado en cliente.

**Frontend — HECHO**

- [x] Nueva sub-pestaña **Recetas** en `PatientRecord`: reusa `PrescriptionSection`
  (envuelta en `grid-column: 1 / -1`).
- [x] Nueva sub-pestaña **Cronología** (primera del expediente): `CronologiaTab`
  fusiona con `Promise.allSettled` citas + notas + recetas + pagos + estudios,
  ordena por fecha desc, filtro `<select>` por tipo, cada fila enlaza a su
  pestaña de detalle. Best-effort: si una fuente falla las demás se muestran.
- [x] `AppointmentsTab` → usa `agendaApi.listByPatient`.

---

### P1c — Sincronización de datos tipados desde la plantilla — HECHO (2026-09-08)

- **Esfuerzo:** M
- **Migración:** no
- **Riesgo:** bajo (best-effort; JSON mal formado = no-op)

**Idea.** Un campo de plantilla (tabla o texto) puede marcarse en su `schemaJson`
con `clinicalMapping` para volcar su contenido a datos tipados al guardar la
historia. Sólo se reemplazan filas con `source = TEMPLATE`; las capturadas a mano
(`MANUAL`) nunca se tocan.

Forma del mapeo en un elemento del schema:

```json
{ "id": "alergias", "type": "table",
  "clinicalMapping": { "target": "ALLERGY", "primary": 0, "secondary": 1,
                       "tertiary": 2, "defaultCategory": "DRUG" } }
```

- `target`: `ALLERGY` | `CONDITION` | `MEDICATION`.
- `primary` / `secondary` / `tertiary`: índices de columna (default 0/1/2). En un
  campo de texto sólo se usa el valor como `primary`.
- Columnas por tipo: ALLERGY = sustancia / reacción; CONDITION = nombre / CIE-10 /
  fecha de inicio; MEDICATION = nombre / dosis / frecuencia.
- `defaultCategory` (sólo ALLERGY): default `OTHER`.

**Backend** — HECHO

- [x] Puerto `TemplateClinicalDataSyncPort` (`records-domain`) +
  `TemplateClinicalDataSyncAdapter` (`records-infra`, `@Component`, usa
  `ObjectMapper`): parsea `schemaJson` (`pages[].elements[]` + `elements[]`
  legacy), agrupa mapeos por `target`, y para cada `target` mapeado hace
  `deleteByClinicIdAndPatientIdAndSource(TEMPLATE)` + re-inserta de las respuestas
  actuales (`answersJson`; tabla = JSON de `string[][]`, texto = una celda).
  `source = TEMPLATE`, `severity = UNKNOWN`, `status = ACTIVE`, `active = true`;
  fecha inválida → `null`; `target` que no aparece en el schema no se toca.
- [x] `PatientAllergy/Condition/MedicationRepositoryPort` ganan
  `deleteByClinicIdAndPatientIdAndSource(...)` (+ `SpringData` derived + `Sql`).
- [x] `MedicalHistoryService`: usa `templateRepository.findByIdAndClinicId` para
  obtener el `schemaJson` y llama `clinicalDataSync.sync(...)` tras guardar la
  versión. Bean en `RecordsDomainConfig` gana la dependencia.
- [x] `TemplateClinicalDataSyncAdapterTest` (8 casos). `clinerya-records-infra`
  41/41 verde · `clinerya-app` compila.

**Frontend — HECHO (2026-09-08)**

- [x] `ClinicalMappingToolbar` (`components/canvas/`): segunda fila de la barra de
  propiedades del elemento seleccionado en `TemplateModal`, visible sólo para
  `table` / `text` / `textarea`. Selector "Sin mapeo / Alergias / Padecimientos /
  Medicación"; para tablas, un `<select>` por rol de columna (Sustancia·Reacción /
  Padecimiento·CIE-10·Fecha / Medicamento·Dosis·Frecuencia) poblado con
  `element.columns`; para ALLERGY, selector de categoría por defecto. Escribe
  `element.clinicalMapping` en el schema (`serializeSchema` ya lo incluye).
- [x] `ClinicalMapping`, `CLINICAL_MAPPING_TARGET_LABELS`,
  `CLINICAL_MAPPING_CATEGORY_LABELS`, `CLINICAL_MAPPING_COLUMN_ROLES` en
  `records/types.ts`; `clinicalMapping?` en `TemplateElement`.
- [x] `replaceSelectedElement` en `TemplateModal` (reemplazo exacto del campo, sin
  propagar a la multiselección — los índices de columna son propios de su tabla).

**Pendiente aparte (de P1a):** `lastVisitAt` en el resumen clínico — requiere
dependencia cross-módulo a `clinerya-agenda` (endpoint "última cita del paciente").

---

### P2a — Gráficas de signos vitales — HECHO (2026-09-08)

- **Esfuerzo:** S
- **Migración:** no
- **Riesgo:** muy bajo

**Problema.** `VitalSigns` (temperatura, TA, FC, FR, peso, talla, **IMC
calculado**, SpO₂) se guarda por nota con fecha y **nunca se grafica**.

**Backend** — sin cambios

- [x] `clinicalNotesApi.listByPatient` ya devuelve todas las notas con
  `vitalSigns` + `createdAt`. Suficiente; no se añadió endpoint.

**Frontend — HECHO**

- [x] `VitalSignsChart` (`components/`): carga las notas del paciente y dibuja
  sparklines SVG a mano (sin librería) — Peso, IMC, Presión arterial (sistólica +
  diastólica en una gráfica), Frecuencia cardíaca, SpO₂, Temperatura, Frecuencia
  respiratoria. Solo aparecen las métricas con datos; cada tarjeta muestra último
  valor, unidad y fecha del último registro; `<title>` por punto con fecha/valor.
- [x] Sección plegable "Signos vitales" en `PatientHistoryPanel`, antes de
  `ClinicalNotesSection`. Estilos `.vitals-*` en `styles.css`.

---

### P2b — Notas clínicas: legibilidad y uso diario — HECHO (2026-09-08)

- **Esfuerzo:** M
- **Migración:** `V45` (addendums)
- **Riesgo:** bajo

**Problemas.**

- `ClinicalNoteModal` imprime **UUIDs crudos** (`Firmante: {signedByUserId}`,
  ~línea 213). La lista (`ClinicalNotesSection`) dice "Escrita por el staff" /
  "por especialista externo" teniendo `doctorId`.
- La lista solo muestra fecha + estado: sin preview del motivo/diagnóstico, sin
  filtro, sin búsqueda, sin paginación
  (`findByPatientIdAndClinicIdOrderByCreatedAtDesc` devuelve todo).
- Sin autoguardado: cerrar el navegador a media nota pierde lo escrito.
- Una nota **firmada no se puede corregir jamás**. NOM-004 prohíbe borrar pero
  exige poder **agregar un addendum**.
- Sin plantillas de nota por especialidad ni "copiar de la última nota".

**Backend — HECHO**

- [x] `ClinicalNoteResponse` incluye `doctorName` y `signedByName`. Nuevo
  `StaffDirectoryPort` (`records-domain`) + `StaffDirectoryAdapter`
  (`records-infra`, crossmodule, usa `ManageClinicStaffUseCase`): `staffName` por
  id de staff (autor), `userName` por id de usuario (firmante). El controlador los
  resuelve en `toResponse`; si no encuentra el nombre deja `null` y el frontend
  cae al ID/etiqueta.
- [x] **Addendum** (`V45`, idempotente — la tabla ya existía en dev):
  `records.clinical_note_addenda (id, clinical_note_id, patient_id, clinic_id,
  content [cifrado], created_by_user_id, created_by_user_name, ip_address,
  user_agent, created_at)`. Modelo `ClinicalNoteAddendum` +
  `ClinicalNoteAddendumRepositoryPort` (+ `SpringData` + `Sql` inline).
  `ManageClinicalNoteUseCase.addAddendum(noteId, patientId, clinicId,
  requestingUserId, AddendumCommand)` + `getAddenda(...)`. En el servicio: exige
  `note.isSigned()` (si no, `IllegalStateException`), contenido no vacío,
  autoriza escritura; hashea (`sha256`) y registra en `document_signatures`
  (`documentId = addendum.id`). Nunca toca la nota original.
- [x] `ClinicalNoteController`: `POST|GET /{noteId}/addenda` con bitácora de
  acceso. `TransactionalClinicalNoteUseCase` + `RecordsDomainConfig` actualizados.
- [x] `ClinicalNoteServiceTest` (5 casos). `clinerya-records-infra` 43/43 verde ·
  `clinerya-app` compila · V45 dry-run OK.

**Pendiente P2b:** listado con `?query=` + paginación por cursor — de momento el
filtro/buscador va en el cliente (los volúmenes de notas por paciente no lo
justifican todavía).

**Frontend — HECHO**

- [x] `ClinicalNotesSection`: fila con `doctorName` (fallback "Especialista
  externo" / "Staff de la clínica"), badge de estado, **preview de 1 línea**
  (assessment→plan→subjective→objective), buscador y filtro por médico, ambos en
  cliente.
- [x] `ClinicalNoteModal`: firmante por `signedByName` (fallback al ID); autor
  visible; sección **Addendums** en notas firmadas (`GET/POST /{noteId}/addenda`,
  lista + form si hay escritura); autoguardado de borrador en `localStorage` por
  `clinical-note-draft:{patientId}:{noteId|"new"}` (hidrata al abrir, limpia al
  guardar/firmar); botón **"Copiar de la última nota"** (S/O/A/P + signos vitales).
- [x] `ClinicalNoteResponse` (front) + `ClinicalNoteAddendumResponse` +
  `clinicalNotesApi.listAddenda` / `addAddendum`.

---

### P3 — Codificación diagnóstica (CIE-10) — HECHO

- **Esfuerzo:** M
- **Migración:** `V52` (el plan decía `V46`; se corrió la numeración por trabajo
  posterior de seguridad — ver nota de `plan-seguridad.md`).
- **Riesgo:** bajo

**Problema.** `assessment` es texto libre, cero CIE-10. Bloquea reportes
epidemiológicos y cualquier informe a Secretaría de Salud.

**Migración `V52__icd10_catalog_and_note_diagnoses.sql`** (+ `V53`, ver abajo)

- `records.icd10_catalog (code PK, description, chapter, billable BOOLEAN)` — la
  migración solo trae un seed manual de 53 códigos comunes; el catálogo **completo
  y oficial** (14,485 códigos, CAT_DIAGNOSTICOS de la DGIS/Secretaría de Salud,
  https://www.datos.gob.mx/dataset/catalogo_cie_10) se carga aparte — ver
  `Icd10CatalogLoader` más abajo.
- `records.clinical_note_diagnoses (id, clinical_note_id FK, clinic_id, icd10_code
  FK, kind [PRIMARY|SECONDARY], created_at)`.
- `V53__fix_incorrect_hand_seeded_icd10_code.sql`: uno de los 53 códigos a mano
  de `V52` (`M26.6` para "trastornos de la articulación temporomandibular") era
  el código de **ICD-10-CM (EEUU)**, no el de la CIE-10 mexicana (que usa `K07.6`
  para lo mismo) — se detectó al cargar el catálogo oficial y no coincidir. `V53`
  lo borra; el código correcto ya está en el catálogo oficial.

**Backend — HECHO**

- [x] `ManageIcd10CatalogUseCase.search(term, limit)` (min. 2 caracteres, limit
  clamp a 1-50) + `Icd10Controller` (`GET /api/v1/icd10?query=&limit=`, protegido
  por el filtro global `anyRequest().authenticated()`; no cae bajo
  `/clinics/**`/`/patients/**` asi que el `PermissionEnforcementArchTest` no lo
  exige gateado por `CurrentUserResolver` — es catalogo global, no dato de paciente).
- [x] **Diseño**: los diagnosticos se capturan al **firmar** la nota, no al
  crearla/editarla — `SignNoteCommand` gano un cuarto campo `diagnoses:
  List<DiagnosisEntry>` (`icd10Code`, `kind`); `ClinicalNoteService.signClinicalNote`
  valida cada codigo contra el catalogo (`IllegalArgumentException` si no existe),
  exige a lo sumo un `PRIMARY`, y los incluye (ordenados) en
  `calculateClinicalNoteHash` antes de firmar. Persistencia via
  `ClinicalNoteDiagnosisRepositoryPort.saveAll`, nueva tabla ligada a la nota (no
  se modela como columna embebida porque es una lista 0..N).
  `GET .../clinical-notes/{noteId}/diagnoses` para leerlos despues.
- [x] **Catálogo completo**: `Icd10CatalogLoader` (`ApplicationRunner` en
  `clinerya-records-infra/.../config/`) sincroniza en cada arranque el catálogo
  oficial completo desde `data/icd10_catalog_mx.tsv` (recurso empaquetado,
  ~1.7MB, 14,485 filas) vía `INSERT ... ON CONFLICT DO UPDATE` en lotes de 1000 —
  así una actualización futura del catálogo (la DGIS lo revisa) solo implica
  reemplazar el TSV, sin migración nueva. La búsqueda (`Icd10CatalogRepositoryPort
  .search`) y la validación al firmar solo aceptan códigos con `billable = true`
  (mapeado del campo oficial `VALID`), lo que excluye categorías de 3 caracteres
  que exigen mayor especificidad (p. ej. `A00` "Cólera" no es codificable
  directo, solo sus hijas `A00.0/A00.1/A00.9`) y códigos retirados/históricos.
- [ ] `PatientCondition.icd10_code` (P1a) poblarse desde aqui queda pendiente —
  no se automatizo el volcado condicion<->diagnostico de nota firmada.

**Frontend — HECHO**

- [x] `Icd10Autocomplete` (nuevo, `modules/records/components/`): input con
  debounce de 250ms + dropdown, chips con codigo + boton "Hacer principal" /
  quitar. Se muestra editable en `ClinicalNoteModal` solo mientras la nota esta
  en `DRAFT` (mismo gate que el boton "Firmar"); en una nota `SIGNED` se muestra
  en modo solo lectura poblado por `GET .../diagnoses`.
- [x] Verificado end-to-end contra Postgres local: migraciones (V51-V53) aplican
  limpio, Hibernate `ddl-auto: validate` no marca discrepancias, el loader carga
  las 14,485 filas oficiales en cada arranque (log `Icd10CatalogLoader`), ciclo
  completo crear nota → firmar con diagnostico → leer diagnosticos probado por
  API real, codigo invalido y codigo no-billable (categoria `A00`) rechazados
  con 400, busqueda de "covid"/"temporomaxilar" devuelve los codigos oficiales
  correctos, y la UI probada en navegador (buscar, agregar, cambiar principal,
  ver chips en nota firmada).

**Pendiente (no bloqueante, fuera de alcance de este corte):** volcar el
diagnostico principal a `PatientCondition` automaticamente; UI para exportar/
filtrar notas por codigo CIE-10 (util para el reporte epidemiologico que motiva
este punto, pero es una pantalla nueva, no una extension de lo ya construido).

---

### P4 — PDF con valor probatorio + exportación estándar

- **Esfuerzo:** L
- **Migración:** `V47`
- **Riesgo:** medio (mueve generación de documentos al backend)

**Problema.** El PDF se genera en el navegador (`lib/pdfExport.ts`, ~1320
líneas). El documento entregado **no es el artefacto que se hasheó y firmó en el
servidor** → poco peso probatorio. No hay exportación estándar ni entrega formal
al paciente (el derecho ARCO de acceso/portabilidad de la LFPDPPP no está
cubierto por `TemporaryRecordShare`, que es para especialistas).

**Backend**

- [ ] Servicio de render server-side (nota firmada, historia firmada, receta,
  expediente completo) → PDF; guardar `document_hash` del PDF en
  `records.rendered_documents (id, clinic_id, patient_id, doc_type, source_id,
  hash, storage_key, created_at)` (`V47`).
- [ ] `GET .../clinical-notes/{id}/pdf`, `.../medical-history/{templateId}/pdf`,
  `.../prescriptions/{id}/pdf`, `.../patients/{id}/record.pdf`.
- [ ] **Export ARCO:** paquete del expediente del paciente (JSON + PDFs) generado
  bajo demanda, con registro en la bitácora (`actionType = "EXPORT"`).

**Backend — opcional (fase posterior)**

- Exportación **HL7 FHIR** (`Patient`, `Condition`, `AllergyIntolerance`,
  `MedicationStatement`, `Observation` para vitals, `DocumentReference` para notas)
  o **CDA**. Habilita interoperabilidad con otras instituciones.

**Frontend**

- [ ] `pdfExport.ts` pasa a ser un fallback; el botón "Descargar" pega al endpoint.
- [ ] En el expediente: "Entregar expediente al paciente" (genera el paquete ARCO,
  muestra el hash).

---

## Secuencia recomendada

```
P0 (seguridad recetas)
   │
   ├─ P1a (cabecera clínica + alergias/crónicos/medicación)   ← seguridad del paciente
   ├─ P1b (pestaña Recetas + Cronología + agenda.listByPatient)
   │
   ├─ P2a (gráficas de signos vitales)     ← máximo valor / mínimo esfuerzo
   ├─ P2b (notas: nombres, preview, filtro, autoguardado, addendum)
   │
   ├─ P3 (CIE-10)
   └─ P4 (PDF server-side + export ARCO / FHIR)
```

- **P0** es un agujero, no una mejora: va primero y solo.
- **P1a + P1b** convierten el expediente en el centro clínico (deja de ser 5 silos).
- **P2a** es el mejor retorno: datos que ya se guardan, sin backend nuevo.
- **P3 y P4** son cumplimiento y escala; priorizables según lo que exijan las
  clínicas / la autoridad.

---

## Propuesta de reordenamiento de la UI

Hoy: `ExpedienteScreen` con 2 pestañas raíz (`Expedientes` / `Plantillas`) →
al elegir paciente, `PatientRecord` con 5 sub-pestañas
(`historia / tratamientos / citas / pagos / estudios`).

| Sub-vista | Enfoque | Nuevo |
|---|---|---|
| **Cronología** | Todo lo del paciente ordenado por fecha | Fusión citas+notas+recetas+estudios+pagos · filtro por tipo · enlace a detalle |
| **Historia clínica** | Plantillas + notas SOAP | Gráficas de signos vitales (plegable) · addendums en notas firmadas · preview + filtro en la lista de notas |
| **Recetas** | Medicación prescrita | `PrescriptionSection` traído al expediente · recuadro pasivo de alergias a fármacos registradas (solo lectura, sin alertas) |
| **Tratamientos** | Procedimientos vinculados a cotizaciones | (sin cambio) |
| **Citas** | Historial de citas del paciente | `agendaApi.listByPatient` en vez de ±2 años client-side |
| **Pagos** | Tickets del paciente | (sin cambio) |
| **Estudios** | Rayos X, laboratorios, etc. | (sin cambio) |

Transversal a todas: **`PatientClinicalHeader`** (alergias · crónicos ·
medicación activa · tipo de sangre · última consulta) fija arriba, con alergias
en rojo.

El modelo de acceso ya existente (`RecordsAccessAuthorizationAdapter`) gatea todo:
`ASSISTANT` ve en solo lectura, `RECEPTIONIST`/`ACCOUNTANT`/`CLEANING` no entran,
y los grants de colaboración externos respetan su nivel.
