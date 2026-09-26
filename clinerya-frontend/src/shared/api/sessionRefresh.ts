// Endpoints de sesion: un 401 aqui significa credenciales malas o cookie vencida, no un token
// de acceso vencido. Renovar en ellos provocaria bucles o reintentos sin sentido.
const SESSION_PATHS = new Set(["/v1/auth/refresh", "/v1/auth/login", "/v1/auth/logout"]);

/** El backend responde 401 cuando no hay sesion valida; 403 es falta de permiso y no se renueva. */
export function shouldRefresh(path: string, status: number): boolean {
  return status === 401 && !SESSION_PATHS.has(path);
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
