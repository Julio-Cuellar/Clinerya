// Endpoints de sesion: un 401 aqui significa credenciales malas o cookie vencida, no un token
// de acceso vencido. Renovar en ellos provocaria bucles o reintentos sin sentido.
const SESSION_PATHS = new Set(["/v1/auth/refresh", "/v1/auth/login", "/v1/auth/logout"]);

/** El backend responde 401 cuando no hay sesion valida; 403 es falta de permiso y no se renueva. */
export function shouldRefresh(path: string, status: number): boolean {
  return status === 401 && !SESSION_PATHS.has(path);
}

/**
 * Lee el {@code exp} del JWT (sin validar la firma: eso lo hace el backend) para saber si hay que
 * renovarlo antes de usarlo donde no se puede reintentar ante un 401, como el CONNECT del chat
 * en vivo. Sin token o con uno ilegible devuelve true: que la renovacion decida.
 */
export function isTokenExpiring(token: string | null, nowMs: number, marginMs: number): boolean {
  const exp = token ? readExpiration(token) : null;
  return exp === null || exp * 1000 - marginMs <= nowMs;
}

function readExpiration(token: string): number | null {
  const payload = token.split(".")[1];
  if (!payload) return null;
  try {
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const bytes = Uint8Array.from(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, "=")), (c) => c.charCodeAt(0));
    const claims = JSON.parse(new TextDecoder().decode(bytes)) as { exp?: unknown };
    return typeof claims.exp === "number" ? claims.exp : null;
  } catch {
    return null;
  }
}

/**
 * Devuelve una funcion que renueva el token de acceso. Si varias peticiones vencen a la vez
 * (el dashboard lanza muchas juntas) comparten la misma llamada a /auth/refresh. Si la
 * renovacion falla, se avisa una sola vez con {@code onExpired} para cerrar la sesion.
 */
export function createSessionRefresher(
  doRefresh: () => Promise<string>,
  onExpired: () => void
): () => Promise<string> {
  let inFlight: Promise<string> | null = null;
  return () => {
    if (!inFlight) {
      inFlight = doRefresh()
        .catch((error: unknown) => {
          onExpired();
          throw error;
        })
        .finally(() => {
          inFlight = null;
        });
    }
    return inFlight;
  };
}
