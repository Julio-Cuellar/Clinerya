# Plan de implementación — Endurecimiento de seguridad

Fecha: 2026-09-08
Estado: propuesta

## Contexto

Auditoría de la rama `feat/nomina-permisos-y-reestructura` sobre autenticación,
autorización, manejo de secretos, criptografía, subida de archivos, exposición de
endpoints y configuración de despliegue.

El sistema procesa **datos personales sensibles de salud** (expedientes, CURP,
notas clínicas, recetas), lo que lo pone bajo LFPDPPP y NOM-004/NOM-024. Eso eleva
la severidad de cualquier fuga o acceso indebido respecto a una app de negocio común.

La base de seguridad no es mala: BCrypt con bloqueo de cuenta, recuperación de
contraseña bien diseñada (`SecureRandom` 32 bytes, token hasheado en BD, expiración
validada, sin enumeración), `clinerya-records` con autorización en la capa de
dominio, cero SQL injection (todo JPA parametrizado), `GlobalExceptionHandler` que
no filtra stack traces y puertos de Docker ligados a `127.0.0.1`.

El problema es la **cobertura desigual**: lo que se blindó se blindó bien, pero
módulos enteros quedaron fuera del modelo de autorización, y hay secretos reales en
el árbol de trabajo y en el historial de git.

### Arquitectura de seguridad vigente

| Capa | Ubicación | Qué cubre |
|---|---|---|
| Filtro de rate limit | `clinerya-auth-infra/.../security/RateLimitingFilter.java` | 8 rutas públicas, por IP del salto de proxy confiable (P2) |
| Filtro JWT | `clinerya-auth-infra/.../security/JwtAuthenticationFilter.java` | Firma + blacklist; puebla `SecurityContext` con el email |
| Cadena Spring Security | `clinerya-auth-infra/.../security/SecurityConfig.java` | Lista de `permitAll` + `anyRequest().authenticated()` |
| Emisión/validación de token | `clinerya-auth-infra/.../adapters/out/JwtTokenProvider.java` | HS384, secreto mínimo 48 bytes, claim `type` ACCESS/REFRESH (P1), access de 30 min (P2) |
| Blacklist de logout | `clinerya-auth-infra/.../adapters/out/SqlTokenRepository.java` | `core.revoked_tokens`, SHA-256 del token, barrido horario (P2) |
| Permiso de plataforma | `users.users.platform_admin` + `PlatformAccessDeniedException` (`clinerya-core`) | Config global y respaldos (`/api/v1/system-configs`), fuera del ámbito de clínica (P1) |
| Membresía de clínica | `clinerya-app/.../security/ClinicAccessInterceptor.java` + `ClinicAccessWebConfig.java` | Solo `/api/v1/clinics/**`; pregunta "¿eres staff activo?" sin mirar rol |
| Permisos granulares | `StaffPermission` (movido a `clinerya-core/.../security` en P3) | Aplicado en los 27 controladores de `/api/v1/clinics/**` y `/api/v1/patients/**` — ver ArchTest de abajo |
| Acceso a expediente | `clinerya-records-infra/.../crossmodule/RecordsAccessAuthorizationAdapter.java` | `PatientAccessAuthorizationPort` (movido a `clinerya-core` en P1): rol de staff + grants externos; lo usan expediente, tratamientos y adjuntos |
| Cifrado de campo | `clinerya-records-infra/.../persistence/security/AesCryptoConverter.java` (duplicado en `clinerya-integrations-infra`) | AES-GCM con prefijo `ENC_GCM:`, lectura legacy `ENC:` |
| Bitácora de accesos | `clinerya-records-domain/.../service/RecordAccessLogService.java` + outbox | Lecturas de expediente con IP y user-agent |
| Reverse proxy | `Caddyfile` (TLS/Let's Encrypt), `nginx.conf` | HSTS, nosniff, DENY, Referrer/Permissions-Policy y CSP en Report-Only (P5) |
| Migraciones Flyway | `clinerya-app/src/main/resources/db/migration/` — de `V46` a `V51` creadas por este plan; **siguiente libre: `V52`** | |

### Cobertura real del modelo de permisos

`grep` de `hasPermission|requirePermission|StaffPermission` por módulo (excluyendo
tests y `target/`):

| Módulo | Archivos con chequeo de permisos |
|---|---|
| `clinerya-staff` | 4 servicios de dominio + adaptador |
| `clinerya-records` | 0 (usa `StaffRole`, no `StaffPermission`) |
| `clinerya-treatments` | **0 — sin ninguna autorización** |
| `clinerya-cash` | 0 |
| `clinerya-inventory` | 0 |
| `clinerya-accounting` | 0 |
| `clinerya-agenda` | 0 |
| `clinerya-clinics` | 0 |
| `clinerya-notifications` | 0 |
| `clinerya-collaboration` | 0 |
| `clinerya-integrations` | 0 |

No existe `@EnableMethodSecurity` ni un solo `@PreAuthorize` en el proyecto.

---

## Hallazgos bloqueantes — resolver antes que nada

### B1. Dump completo de la base de datos para cualquier autenticado

`SystemConfigController.java:68` expone `POST /api/v1/system-configs/backups/trigger`,
que ejecuta `pg_dump` de **toda** la BD y la transmite al cliente HTTP.

Ese path no cuelga de `/api/v1/clinics/**`, así que `ClinicAccessInterceptor` no lo
toca; no hay method security; y `SecurityConfig.java:47` solo exige `.authenticated()`.
Cualquier cuenta —una recepcionista, un asistente, un staff de otra clínica— se lleva
los expedientes de **todas** las clínicas del tenant con un solo POST.

Los mismos `GET`/`PUT /api/v1/system-configs/{key}` (`:45`, `:52`) leen y escriben
configuración global sin restricción.

### B2. Secretos reales en el árbol de trabajo y en el historial

En `clinerya-app/src/main/resources/application.yml` (archivo trackeado, modificado):

- **`:110`** — client secret de Google OAuth en claro como default:
  `client-secret: ${GOOGLE_OAUTH_CLIENT_SECRET:GOCSPX-...}`
- **`:105`** — el default de `encryption-key` es el **client ID de Google**
  (`670646533749-...apps.googleusercontent.com`). Parece un copy-paste cruzado: además
  de ser un valor público, si algún entorno arranca sin `MEDICLOUD_ENCRYPTION_KEY`
  cifraría PHI con una llave conocida.

Y en **HEAD** (commit `a62091a "fase beta"`, ya empujado a
`github.com/Julio-Cuellar/Clinerya`) sigue el fallback del JWT:

```yaml
jwt:
  secret: ${JWT_SECRET:i_wUbYfIZ...}
```

Con ese valor cualquiera forja un JWT válido para cualquier usuario. **Trátalo como
comprometido**, sin importar si el repo es privado.

### B3. `clinerya-treatments` sin ninguna autorización

Tres controladores expuestos solo con `.authenticated()`:

| Endpoint | Archivo |
|---|---|
| `/api/v1/patients/{patientId}/quotations/**` | `QuotationController.java:40` |
| `/api/v1/patients/{patientId}/visits/**` | `GeneralVisitController.java:30` |
| `/api/v1/patients/{patientId}/quotations/{quotationId}/visits/**` | `VisitController.java:27` |

El `clinicId` llega en el body/query y nunca se contrasta contra la membresía del
llamante, lo que abre un IDOR entre clínicas sobre cotizaciones y visitas. Además
`createQuotation` toma `request.createdByUserId()` del body: la autoría es
falsificable.

Es el mismo patrón corregido en `ClinicStaffController` (2026-09-05) y en recetas
(P0 del plan de expediente). Falta replicarlo aquí.

---

## Fases

### P0 — Contención de secretos

- **Esfuerzo:** S (pero urgente y con coordinación externa)
- **Migración:** no
- **Riesgo:** medio — rotar `MEDICLOUD_ENCRYPTION_KEY` rompe el descifrado de datos ya cifrados

**Código — HECHO (2026-09-09)**

- [x] **`application.yml:105`** — el default de `encryption-key` era un client ID de
      Google pegado por error. Fallback eliminado: sin `MEDICLOUD_ENCRYPTION_KEY` la
      aplicación ya no arranca. Comentario explicando el porqué y la advertencia de
      `Tag mismatch`.
- [x] **`application.yml:110`** — quitado el client secret de Google en claro
      (`GOCSPX-...`). Era una edición **sin commitear**: nunca llegó al historial, así
      que el archivo volvió al estado de HEAD (`${GOOGLE_OAUTH_CLIENT_SECRET:}`).
- [x] **Default hardcodeado en Java** — `AesCryptoConverter` traía
      `@Value("${medicloud.security.encryption-key:DefaultSecretEncryptionKey32Chars!}")`,
      que anulaba el "falla al arrancar" del YAML. Eliminado en **las dos copias**
      (`records-infra` e `integrations-infra`). Adelanta parte de P4.
- [x] **Seeds a `false` por default** (`app.dev-seed.enabled`,
      `app.showcase-seed.enabled`). Venían en `true`: cualquier arranque fuera de
      docker-compose creaba cuentas activas con contraseña publicada en el repo.
- [x] **Auditoría del resto del YAML** — `MEDICLOUD_DB_PASSWORD`, `jwt.secret` y
      `google.oauth.state-secret` ya estaban sin fallback en el árbol de trabajo.
      Queda `RABBITMQ_USERNAME/PASSWORD: guest` como default de desarrollo: RabbitMQ
      rechaza `guest` fuera de localhost y docker-compose lo sobrescribe, así que se
      deja para no romper el arranque local. Reevaluar si algún día se expone el broker.
- [x] **Escaneo de secretos en CI** — nuevo `.github/workflows/secret-scan.yml`
      (gitleaks). Va aparte de `ci.yml` porque ese solo dispara en `main` y `Cuellar`,
      y el escaneo debe correr en todas las ramas y PRs.
- [x] Verificado: YAML parsea, `grep` de patrones de secreto sobre archivos trackeados
      sale limpio, y `records-infra` + `integrations-infra` compilan.

**Operativo — pendiente (solo lo puedes hacer tú)**

- [ ] **Rotar `JWT_SECRET`** en el `.env` de producción (mínimo 64 bytes aleatorios).
      Sigue en HEAD (commit `a62091a`, ya empujado), así que está publicado.
      Invalida todas las sesiones activas: avisar antes o hacerlo en ventana baja.
- [ ] **Rotar el client secret de Google OAuth** desde Google Cloud Console.
      Aunque no llegó al historial, estuvo en disco y en esta conversación.
- [ ] **Decidir sobre el historial de git**: purgar con `git filter-repo` / BFG (reescribe
      hashes, requiere coordinar con cualquier clon existente) o asumir la fuga y
      confiar solo en la rotación. Con un equipo chico, rotar y limpiar HEAD suele ser
      suficiente; documentar la decisión.
- [ ] **Trackear `application-local.yml`** o documentarlo en el README: ahora que no hay
      fallbacks, un clon nuevo no arranca en local sin ese archivo, y hoy está sin
      commitear. Su contenido es solo de desarrollo y el propio archivo lo explica.

> **Nota sobre `MEDICLOUD_ENCRYPTION_KEY`:** no la rotes a ciegas. Si ya hay columnas
> `ENC_GCM:` en producción, cambiarla provoca `Tag mismatch` en `AesCryptoConverter`
> (advertencia ya documentada en `application-local.yml`). La rotación real necesita el
> re-cifrado de P4.

---

### P1 — Cierre de la superficie crítica — HECHO (2026-09-09)

- **Esfuerzo:** M
- **Migración:** sí — `V46__user_platform_admin.sql`
- **Riesgo:** bajo — son chequeos aditivos

**B1 · `system-configs`**

- [x] **Permiso de plataforma** modelado como columna `users.users.platform_admin`
      (`V46`), distinto de `StaffPermission` porque esos son por clínica y esto es global
      al tenant. Se otorga manualmente en BD: ningún flujo de registro puede activarlo.
- [x] Propagado por el modelo: `User.platformAdmin`, `UserEntity`, y el mapper anónimo de
      `UserDomainConfig` en ambos sentidos.
- [x] Nueva `PlatformAccessDeniedException` en `clinerya-core/security` —separada de
      `ClinicAccessDeniedException` para que un chequeo de plataforma no se "arregle"
      concediendo membresía de clínica— con su handler → HTTP 403.
- [x] `SystemConfigController.requirePlatformAdmin()` en los tres endpoints. En
      `triggerBackup` el chequeo va **fuera** del `try`: el `catch (Exception)` de ese
      método habría convertido el 403 en un 500 genérico.
- [x] Bitácora: se registra actor en la actualización de config y en cada disparo de
      respaldo.
- [ ] Pendiente (no bloqueante, mover a P5): sacar `backups/trigger` del API HTTP hacia
      un job operativo con descarga firmada de duración corta.

**B3 · `clinerya-treatments`**

- [x] **`PatientAccessAuthorizationPort` promovido a `clinerya-core/security`**, junto a
      `ClinicMembershipPort`. `treatments-domain` solo dependía de `core`, así que
      hacerlo depender de `records-domain` habría acoplado dos dominios. 15 archivos
      actualizados; `RecordsAccessAuthorizationAdapter` sigue siendo la única
      implementación.
- [x] Autorización **en los servicios de dominio** (`QuotationService.authorize()`,
      `VisitService.authorize()`), mismo patrón que `MedicalHistoryService`, para que un
      endpoint nuevo no pueda saltársela.
- [x] Nuevo `QuotationLookupUseCase` (solo lectura, sin authz) para agenda y caja, que
      validan contra una cotización sin usuario en la petición. Antes usaban
      `ManageQuotationUseCase` directamente. Documentado como no exponible desde un
      controlador; verificado con grep que ningún controlador lo usa.
- [x] `createQuotation` toma el autor de `CurrentUserResolver`. `createdByUserId`
      eliminado del DTO de entrada y del frontend (`quotationTypes.ts`,
      `QuotationEditorScreen.tsx`): era falsificable desde el body.
- [x] `clinerya-treatments-infra` ahora depende de `clinerya-users-infra`
      (`CurrentUserResolver`), igual que `records-infra`.
- [x] Seeders y `TransactionalVisitUseCase` propagan el usuario que actúa.
- [x] `QuotationServiceAuthorizationTest` (6 casos): llamante de otra clínica → 403 en
      lectura y escritura; grant externo `READ_ONLY` → lee pero no escribe; sin usuario →
      403; autoría tomada del autenticado; el lookup de sistema no autoriza.

**Tokens JWT**

- [x] `validateToken` reemplazado por `validateAccessToken` / `validateRefreshToken`.
      Todo token lleva `type` explícito (`ACCESS` / `REFRESH`) y la validación exige el
      esperado: un refresh ya no sirve como bearer, ni un access para renovar.
- [x] `RefreshTokenService` consulta la blacklist.
- [x] Dos huecos que hacían inútil ese chequeo, encontrados al implementarlo:
  - El refresh token **nunca se blacklisteaba** (no viaja en la cabecera). `logout` ahora
    acepta el refresh en el body y el frontend lo manda.
  - `LogoutService` retenía **24 h fijas**, menos que los 7 días de un refresh: pasada la
    entrada, el token revivía. La retención se deriva ahora del propio token vía
    `TokenProviderPort.millisUntilExpiry`.
- [x] `TokenTypeAndBlacklistTest` (6 casos) en el módulo `auth`, que no tenía tests.

**Verificado:** `mvn clean test` en verde (128 tests, BUILD SUCCESS) y `tsc --noEmit`
limpio.

> **Al desplegar:** exigir el claim `type` invalida los access tokens ya emitidos (no lo
> llevaban), así que todas las sesiones activas se cierran. Conviene desplegarlo **junto
> con la rotación de `JWT_SECRET` de P0**, que provoca exactamente el mismo efecto: así es
> un solo cierre de sesión y no dos.

---

### P2 — Sesión y rate limiting — HECHO (2026-09-09)

- **Esfuerzo:** M
- **Migración:** sí — `V47__revoked_tokens.sql`
- **Riesgo:** bajo

- [x] **Rate limit evadible.** Tomaba el primer valor de `X-Forwarded-For`, que lo escribe
      el cliente: bastaba con mandar una IP inventada distinta en cada intento para
      estrenar bucket. Ahora se cuenta **desde la derecha**, que es lo que añaden los
      proxies (`$proxy_add_x_forwarded_for` en nginx, `reverse_proxy` en Caddy), con
      `medicloud.security.trusted-proxy-count` (default 1, que corresponde a las dos
      topologías del repo). Si hay menos saltos de los esperados cae a `getRemoteAddr()`
      en lugar de confiar en el cliente.
- [x] **Cobertura ampliada** de 3 a 8 rutas: `register`, `verify-email`,
      `resend-verification`, `register-staff` y `public/shared-history`.
- [x] **Blacklist persistente** (`V47`, `core.revoked_tokens`): `SqlTokenRepository`
      sustituye a `InMemoryTokenRepository`, que perdía todos los logouts en cada
      reinicio. Se guarda el **SHA-256** del token, no el token, y hay barrido horario de
      entradas expiradas.
- [x] **Access token de 24 h → 30 min** (`jwt.expiration`, configurable por
      `JWT_EXPIRATION_MS`). El frontend ya renueva solo al recibir un 401, así que no se
      nota en uso normal.
- [x] **Enumeración de usuarios en login**: mensaje único hasta que se comprueba la
      contraseña; los mensajes específicos ("no activo", "bloqueada") solo se dan a quien
      ya demostró conocerla. Añadido un hash de descarte para gastar el mismo tiempo de
      bcrypt cuando el correo no existe — sin eso el mensaje unificado no sirve, porque el
      reloj delata la diferencia. Una cuenta ya bloqueada deja de acumular intentos, para
      que no se pueda extender el bloqueo desde fuera.
- [x] `LoginServiceTest` (7 casos) y `RateLimitingFilterTest` (7 casos, incluida la
      regresión del bypass).

**Verificado:** `mvn clean test` en verde (142 tests) y `tsc --noEmit` limpio.

> **Compromiso asumido:** `isBlacklisted` corre en cada petición autenticada, así que la
> validación de token pasó de memoria a un `SELECT` por request. Es una búsqueda por clave
> primaria y a esta escala no se nota; si algún día pesa, la mitigación es una caché
> negativa de TTL corto (segundos) delante del repositorio, no volver a memoria.

> **Verificar en producción:** `trusted-proxy-count: 1` asume **un** proxy delante del
> backend. En este repo Caddy va directo a `backend:8082` y nginx también, así que 1 es
> correcto; si algún día metes un balanceador o CDN delante, hay que subirlo o el límite
> volverá a ser evadible.

---

### P3 — Permisos granulares en todos los módulos — HECHO (2026-09-09)

- **Esfuerzo:** L — es el trabajo grande; merece su propia rama
- **Migración:** no
- **Riesgo:** medio-alto — toca firmas de dominio en 8 módulos y puede romper flujos en uso

`StaffPermission` declara ~60 permisos (`VIEW_MEDICAL_RECORDS`, `MANAGE_CASH`,
`VIEW_FINANCIAL_REPORTS`, `MANAGE_INVENTORY`…) pero solo se aplica en `clinerya-staff`.
Para todo lo demás, `ClinicAccessInterceptor` únicamente pregunta *"¿eres staff activo
de esta clínica?"* — sin mirar rol ni permisos. **Cualquier miembro activo puede llamar
contabilidad, caja, inventario, recetas y catálogo.** Fuera del módulo staff, los
permisos son efectivos solo en la UI.

- [x] **Promover `StaffPermissionCheckerPort` a `clinerya-core`** (junto con el enum
      `StaffPermission`) para que todos los módulos lo inyecten sin depender de
      `staff-domain`.
- [x] Aplicado en el dominio, con `actingUserId` explícito y `ClinicAccessDeniedException`
      si falta el permiso:
  - [x] `clinerya-cash` (`MANAGE_CASH_CUTS`, `VIEW_CASH`, `CREATE_CHARGES`, `MANAGE_REFUNDS`, `MANAGE_EXPENSES`)
  - [x] `clinerya-accounting` (`VIEW_ACCOUNTING`, `MANAGE_ACCOUNTING`, `VIEW_JOURNAL_ENTRIES`, `CREATE_JOURNAL_ENTRIES`, `VIEW_FINANCIAL_REPORTS`, `MANAGE_BANK_ACCOUNTS`)
  - [x] `clinerya-treatments` — catálogo de tratamientos (`MANAGE_TREATMENT_CATALOG`, `VIEW_TREATMENTS`); cotizaciones/visitas ya quedaron cubiertas por `PatientAccessAuthorizationPort` en P1
  - [x] `clinerya-inventory` (`MANAGE_MATERIALS`, `VIEW_INVENTORY`, `MANAGE_INVENTORY_MOVEMENTS`, `MANAGE_INVENTORY_LOTS`, `MANAGE_PURCHASES`)
  - [x] `clinerya-agenda` (`CREATE_APPOINTMENTS`, `EDIT_APPOINTMENTS`, `CANCEL_APPOINTMENTS`, `VIEW_AGENDA`, `MANAGE_SCHEDULES`, `MANAGE_ROOMS`, `VIEW_ROOMS`, `MANAGE_WAITING_LIST`)
  - [x] `clinerya-clinics` — consultorios/sillones y asignación de personal (`CREATE_ROOMS`, `EDIT_ROOMS`, `MANAGE_ROOMS`, `VIEW_ROOMS`, `ASSIGN_ROOM_STAFF`); `ManageClinicUseCase` no se tocó porque ya exige ser el dueño de la clínica, más estricto que cualquier permiso de staff
  - Los procesos internos sin usuario en la petición (listeners de eventos, sincronización con Google Calendar, jobs programados, lookups entre módulos) se separaron en variantes `*ForSystem` explícitas, sin chequeo — documentadas en el código como no exponibles por HTTP.
- [x] **Alinear `RecordsAccessAuthorizationAdapter`**: ya no decide por `StaffRole` puro,
      consulta `getPermissions()` (rol + overrides), así que revocar `VIEW_MEDICAL_RECORDS`
      a un doctor por override ahora sí le quita el acceso.
- [x] **Cerrado `/api/v1/internal/scheduler/material-reservations/run`**: ahora exige
      `user.isPlatformAdmin()`, igual que `/api/v1/system-configs`.
- [x] **Test de arquitectura** (`PermissionEnforcementArchTest`, ArchUnit): falla si un
      controlador nuevo bajo `/api/v1/clinics/**` o `/api/v1/patients/**` no tiene forma de
      resolver al usuario autenticado (`CurrentUserResolver` o `Principal`). Verificado que
      detecta la violación (se probó quitando una excepción a propósito).

**Hallazgo fuera de alcance, no corregido:** `StaffCompensationController`,
`StaffOnboardingController`, `StaffOperationsController` (staff), y
`ExternalCalendarEventController`/`GoogleCalendarController` (integrations) y
`NotificationController` (notifications) están bajo `/api/v1/clinics/**` pero no aplican
`StaffPermission` — quedaron fuera de la lista de módulos de esta fase. Los tres de staff
resuelven al usuario vía `Principal` (no vía permiso); los tres restantes no resuelven al
usuario en absoluto. Documentados como excepción explícita en el test de arquitectura para
que no se degrade más, pero siguen abiertos.

---

### P4 — Criptografía y expedientes compartidos — PARCIAL (2026-09-10)

- **Esfuerzo:** M-L
- **Migración:** sí — `V48__temporary_record_shares_hardening.sql`
- **Riesgo:** alto en el re-cifrado — requiere respaldo verificado y ventana de mantenimiento

**Hecho salvo el re-cifrado masivo** (decisión explícita: no tocar datos cifrados
existentes en esta pasada). Los registros `ENC_GCM:` y `ENC:` se siguen leyendo por su
ruta legacy; las escrituras nuevas usan `ENC_GCM_V2:`.

**`FieldCipher`** (`clinerya-core/.../security/crypto/`):

- [x] **Unificado en `clinerya-core`**. Los `@Converter` de `records-infra` e
      `integrations-infra` son adaptadores finos que delegan; el algoritmo y el formato
      viven una sola vez y no pueden divergir.
- [x] **Sin default hardcodeado** — ya se había quitado del constructor en P0; el
      `application.yml` no tiene fallback para `encryption-key`.
- [x] **KDF**: llave v2 derivada por PBKDF2-HMAC-SHA256, 210 000 iteraciones, 256 bits,
      sobre el material de binding (`machine_id:product_uuid:passphrase` o
      `.field-key:passphrase`). El copiado crudo con relleno de ceros solo sobrevive para
      leer datos v1.
- [x] **`.field-key` fuera de `/app/uploads`** — nuevo `medicloud.security.key-store-dir`
      (`MEDICLOUD_KEY_STORE_DIR`, default `/app/secrets`). Migración transparente: si existe
      el `.secure_key` viejo se copia, nunca se regenera. Permisos `600` donde hay POSIX.
- [x] **Falla en vez de degradar**: un valor con prefijo que no descifra lanza
      `FieldCryptoException` en vez de devolver el ciphertext. Un valor sin prefijo se
      devuelve tal cual pero incrementa `FieldCipher.getPlaintextReads()` (observable).
- [x] **Rotación habilitada**: prefijo versionado `ENC_GCM_V2:`. El re-cifrado masivo
      `ENC:`/`ENC_GCM:` → `ENC_GCM_V2:` y el borrado de `legacyDecrypt` quedan como
      runbook para la ventana de mantenimiento (junto con la rotación real de
      `MEDICLOUD_ENCRYPTION_KEY`).

**`/api/v1/public/shared-history`** (`TemporaryRecordShareController`), `V48`:

- [x] **Token fuera del query string**: ahora es `POST /api/v1/public/shared-history` con
      el token en el cuerpo. La ruta `permitAll` ya cubría el método.
- [x] **Solo el hash del token** en BD (`token_hash`, SHA-256 base64url), mismo criterio
      que `PasswordResetService`. La columna `token` en claro se eliminó; los enlaces
      vigentes se borraron en la migración (efímeros, <=30 días, sin token en claro
      recuperable).
- [x] **Revocación**: columna `revoked_at` + `GET`/`DELETE
      /api/v1/clinics/{clinicId}/patients/{patientId}/temporary-shares`. Pantalla de
      gestión en el frontend (`ManageTemporarySharesModal`, botón "Enlaces temporales"
      en el panel de historia clínica) — commits `e32b639` / `<commit B>`.
- [x] **Cada consulta se registra en `RecordAccessLog`** vía el outbox
      (`resourceType=TEMPORARY_SHARE` / `TEMPORARY_SHARE_STUDY`, `actionType=VIEW`,
      `userId=null`), más `last_accessed_at`/`access_count` en la fila. `V49` volvió
      `record_access_log(_outbox).user_id` NULLABLE porque una consulta por enlace no
      tiene usuario interno.
- [x] **Selector de secciones al generar** (`shared_sections` CSV, `V50`; enum
      `SharedSection`): notas SOAP, historia clínica (formularios con tablas y
      odontograma renderizados), signos vitales, prescripciones, y **estudios**
      (adjuntos `patient_studies`) vía `POST /api/v1/public/shared-history/studies/{id}/content`
      — sirve el binario como `attachment`+`nosniff`, rate limit por prefijo, puerto
      `SharedStudyLookupPort` sobre el módulo de adjuntos. Commits `e32b639` (4 secciones)
      + commit B (estudios).
- [x] **Verificar al destinatario** con un código de un solo uso al correo (`V51`):
      `verification_code_hash`/`verification_code_expires_at`/`verification_attempts`
      (mismo criterio de hash que `token_hash`, sin persistir el código en claro).
      `POST /api/v1/public/shared-history/verify/request` genera y envía el código
      (`EmailSenderPort.sendShareRecipientVerificationCode`, plantilla de texto plano);
      `POST /api/v1/public/shared-history/verify/confirm` lo valida (10 min de vigencia,
      bloqueo tras 5 intentos). `getSharedRecord`/`getSharedStudyContent` exigen
      `recipient_verified_at` no nulo — si falta, lanzan `RecipientVerificationRequiredException`
      (HTTP 428, `code: RECIPIENT_VERIFICATION_REQUIRED`) para que el frontend muestre la
      pantalla de código en vez de un error. `ShareViewScreen` implementa ese flujo.
      Rate limit dedicado en ambos endpoints nuevos.

**Pendiente operativo (ventana de mantenimiento, solo tú):**

- [ ] Re-cifrar `ENC:`/`ENC_GCM:` → `ENC_GCM_V2:` y rotar `MEDICLOUD_ENCRYPTION_KEY`.
- [ ] Montar un volumen dedicado y respaldado en `MEDICLOUD_KEY_STORE_DIR` (`/app/secrets`).
- [ ] Tras confirmar que no quedan filas `ENC:`, borrar `legacyDecrypt` de `FieldCipher`.

---

### P5 — Endurecimiento de plataforma — HECHO (2026-09-09)

- **Esfuerzo:** S-M
- **Migración:** no
- **Riesgo:** bajo — salvo la CSP, que puede romper la UI si se aprieta de golpe

**Cabeceras y frontend**

- [x] `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`,
      `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin` y
      `Permissions-Policy` en `nginx.conf` (a nivel `server`, con `always`) y en
      `Caddyfile`. En Caddy además `-Server` para no anunciar el servidor.
- [x] **CSP en `Report-Only`**, como pedía el plan: primero medir. El perfil se dedujo del
      código real — `style-src 'unsafe-inline'` porque React usa `style={{...}}` en 36
      componentes, `fonts.googleapis/gstatic` por la fuente Inter de `index.html`, e
      `img-src data: blob:` por la firma del paciente (`toDataURL`) y el export a PDF.
      Cuando el reporte salga limpio, renombrar la cabecera a `Content-Security-Policy`.
- [x] **Refresh token movido a cookie `HttpOnly`** (2026-09-13). Antes viajaba en el
      cuerpo JSON de login/refresh/logout y se guardaba en `localStorage`: un XSS que
      leyera `localStorage` se llevaba un token de 7 días. Ahora `AuthController` lo
      entrega como `Set-Cookie` (`HttpOnly`, `Secure` — configurable con
      `medicloud.security.secure-cookies`, `false` solo en `application-local.yml` sin
      TLS —, `SameSite=Strict`, `Path=/api/v1/auth`), `refresh`/`logout` lo leen con
      `@CookieValue` en vez de del cuerpo, y `LoginResponse`/`TokenRefreshResponse` ya
      no lo exponen. **Decisión sobre CSRF:** no se reactivó el filtro CSRF de Spring
      (rompería todo el API autenticado por cabecera `Authorization`); la mitigación es
      `SameSite=Strict`, que evita que el navegador mande la cookie en peticiones
      cross-site — el filtro de rate limit y el hecho de que la respuesta de `/refresh`
      no es legible cross-origin (SOP) acotan aún más el riesgo residual. Frontend:
      `sessionStore` ya no guarda ni lee un refresh token (`fetch` manda la cookie sola
      vía `credentials: "include"`); el reintento en 401 ya no puede comprobar si hay
      refresh token antes de intentar — simplemente lo intenta y limpia la sesión si
      falla.

**Adjuntos**

- [x] **Validación por bytes mágicos** (`%PDF`, `FFD8FF`, cabecera PNG) en vez del
      `Content-Type` de la petición, que lo elige quien sube el archivo. El tipo detectado
      es el que se persiste y se sirve.
- [x] `Content-Disposition: attachment` en vez de `inline` + `X-Content-Type-Options:
      nosniff`. Un PDF servido inline se abre en el visor del navegador desde nuestro
      propio origen, y un PDF puede traer JavaScript. **No rompe el frontend**: usa
      `downloadBlob` con `fetch()`, así que nunca dependió de la cabecera.
- [x] Nombre de archivo según **RFC 6266** (`filename` ASCII + `filename*` UTF-8), con
      separadores de ruta, comillas y caracteres de control saneados. Antes se
      interpolaba sin escapar dentro de las comillas.
- [x] `AttachmentServiceTest` (5 casos) en un módulo que no tenía tests, incluido "HTML
      disfrazado de PNG".

**Verificación de correo**

- [x] El código de 6 caracteres se buscaba **globalmente** entre todos los pre-registros
      pendientes: un acierto al azar activaba la cuenta de cualquiera. Ahora
      `verifyEmail(email, token)` lo acota al correo indicado, así que hay que atacar un
      objetivo concreto. Sumado al rate limit de P2, el ataque deja de ser práctico.
      Frontend actualizado (el correo sale del registro reciente; si se recargó la página,
      de un campo visible).

**Varios**

- [x] `HmacStateCodec`: firma comparada con `MessageDigest.isEqual` (tiempo constante).
- [x] `JwtTokenProvider`: `getBytes(StandardCharsets.UTF_8)` explícito (hecho en P1).
- [x] **Spring Boot 3.3.4 → 3.5.16.** Descartado 4.x: es cambio mayor (Spring Framework 7)
      y sería un proyecto aparte, no un endurecimiento. 3.5.16 es la última con soporte de
      la línea 3.x y arrastra ~2 años de parches de Spring Framework, Tomcat y Spring
      Security.
- [x] **Escaneo de dependencias en CI**: nuevo `.github/workflows/dependency-scan.yml`
      (osv-scanner para Maven + `npm audit --audit-level=high`), en todas las ramas y
      además **semanal por cron** — una dependencia no cambia, pero la base de CVEs sí.
- [x] `npm audit` reportaba 2 vulnerabilidades transitivas de build (browserslist:
      alta, DoS/OOM; baseline-browser-mapping: moderada). Ninguna llega al bundle.
      Resueltas con `npm audit fix` sin cambios breaking; ahora sale en 0.

**Verificado:** `mvn clean test` en verde (147 tests) bajo Spring Boot 3.5.16,
`tsc --noEmit` limpio, `npm run build` OK y `npm audit` en 0.

> **El suite de tests NO prueba que la aplicación arranque.** Son todos tests unitarios de
> servicios de dominio: no existe ni un `@SpringBootTest`. Un salto de dos versiones menores
> de Spring Boot puede romper el cableado de beans o el arranque de Flyway/JPA sin que
> ningún test se entere. **Levanta la app una vez antes de desplegar.** Y considera añadir
> un smoke test que cargue el contexto: es justo lo que faltaba para validar este upgrade.

> **`nginx.conf` está en `.gitignore`** (línea 57), así que las cabeceras que acabo de
> añadirle **no están versionadas**: quien redespliegue desde un clon limpio las pierde en
> silencio. `Caddyfile` está sin commitear pero no ignorado. Conviene sacar `nginx.conf` del
> `.gitignore` — no contiene secretos — y commitear ambos.

---

## Secuencia recomendada

```
P0 (secretos) → P1 (superficie crítica) → P2 (sesión) → P5 (plataforma)
                                        ↘ P3 (permisos, rama aparte)
                                        ↘ P4 (cripto + compartidos)
```

- **P0 va primero y solo.** Es de horas y no depende de nada. Mientras el
  `JWT_SECRET` publicado siga siendo válido, todo lo demás es teatro.
- **P1 puede entrar en la misma rama que P0** si quieres un solo despliegue de
  contención. Son chequeos aditivos y de bajo riesgo.
- **P2 y P5** son independientes entre sí; cualquiera de las dos puede ir en paralelo
  a P3.
- **P3 merece rama propia.** Toca 8 módulos y firmas de dominio; mezclarlo con la
  contención hace el diff irrevisable.
- **P4 al final** porque el re-cifrado necesita ventana de mantenimiento y respaldo
  verificado, y porque la rotación de `MEDICLOUD_ENCRYPTION_KEY` que P0 deja pendiente
  se cierra aquí.

## Riesgo residual aceptado (a revisar tras P0-P2)

- Sin WAF ni detección de anomalías: un token robado válido se ve igual que uso legítimo.
- Sin MFA para roles con acceso a expedientes.
- Los respaldos (`pg_dump`) salen en claro; el cifrado a nivel campo protege columnas
  puntuales, no el volcado completo.
- La bitácora de accesos cubre `records`, no el resto de módulos.

## Lo que ya está bien (no tocar)

Registrado para que no se pierda en futuras revisiones:

- **`PasswordResetService`** — `SecureRandom` de 32 bytes, token hasheado con SHA-256
  en BD, expiración de 30 min validada en `User.resetPassword`, respuesta genérica sin
  enumeración. Es el patrón de referencia para P4.
- **`MedicalHistoryService.authorize()`** — autorización en el dominio, no en el
  controlador. Es el patrón de referencia para P1 y P3.
- **BCrypt** con bloqueo de cuenta por intentos fallidos.
- **Cero SQL injection**: todo JPA y `@Query` parametrizado, sin concatenación.
- **`GlobalExceptionHandler`** no filtra stack traces al cliente.
- **`docker-compose.yml`**: puertos en `127.0.0.1`, secretos obligatorios con `:?`,
  `ddl-auto: validate`.
- **`RecordAccessLog`** con outbox, métricas y health indicator.
