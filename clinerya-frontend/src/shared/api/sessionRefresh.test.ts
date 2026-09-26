import { describe, expect, it, vi } from "vitest";
import { createSessionRefresher, isTokenExpiring, shouldRefresh } from "./sessionRefresh";

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe("shouldRefresh", () => {
  it("renueva ante un 401 de cualquier endpoint del API", () => {
    expect(shouldRefresh("/v1/clinics/abc/patients", 401)).toBe(true);
  });

  it("no renueva ante otros codigos: un 403 es falta de permiso, no sesion vencida", () => {
    expect(shouldRefresh("/v1/clinics/abc/patients", 403)).toBe(false);
    expect(shouldRefresh("/v1/clinics/abc/patients", 500)).toBe(false);
  });

  it("no renueva para los endpoints de sesion: evita bucles y reintentos de credenciales malas", () => {
    expect(shouldRefresh("/v1/auth/refresh", 401)).toBe(false);
    expect(shouldRefresh("/v1/auth/login", 401)).toBe(false);
    expect(shouldRefresh("/v1/auth/logout", 401)).toBe(false);
  });
});

describe("createSessionRefresher", () => {
  it("varias peticiones vencidas a la vez comparten una sola renovacion", async () => {
    const pending = deferred<string>();
    const doRefresh = vi.fn(() => pending.promise);
    const refresh = createSessionRefresher(doRefresh, vi.fn());

    const calls = [refresh(), refresh(), refresh()];
    pending.resolve("nuevo");

    expect(await Promise.all(calls)).toEqual(["nuevo", "nuevo", "nuevo"]);
    expect(doRefresh).toHaveBeenCalledTimes(1);
  });

  it("terminada una renovacion, la siguiente vez vuelve a renovar", async () => {
    const doRefresh = vi.fn().mockResolvedValueOnce("primero").mockResolvedValueOnce("segundo");
    const refresh = createSessionRefresher(doRefresh, vi.fn());

    expect(await refresh()).toBe("primero");
    expect(await refresh()).toBe("segundo");
    expect(doRefresh).toHaveBeenCalledTimes(2);
  });

  it("si la renovacion falla, avisa una sola vez que la sesion vencio y todas las esperas fallan", async () => {
    const pending = deferred<string>();
    const onExpired = vi.fn();
    const refresh = createSessionRefresher(() => pending.promise, onExpired);

    const calls = [refresh(), refresh()].map((call) => call.then(() => "ok", () => "fallo"));
    pending.reject(new Error("cookie vencida"));

    expect(await Promise.all(calls)).toEqual(["fallo", "fallo"]);
    expect(onExpired).toHaveBeenCalledTimes(1);
  });

  it("tras un fallo no se queda atascado: el siguiente intento vuelve a llamar al servidor", async () => {
    const doRefresh = vi.fn().mockRejectedValueOnce(new Error("red caida")).mockResolvedValueOnce("recuperado");
    const refresh = createSessionRefresher(doRefresh, vi.fn());

    await expect(refresh()).rejects.toThrow("red caida");
    expect(await refresh()).toBe("recuperado");
  });
});

describe("isTokenExpiring", () => {
  const NOW = Date.UTC(2026, 8, 26, 12, 0, 0);
  const MARGIN = 30_000;

  // Solo importa el payload: la firma la valida el backend, no el navegador.
  function jwtExpiringAt(epochMs: number, extra: Record<string, string> = {}): string {
    const payload = JSON.stringify({ sub: "u1", ...extra, exp: Math.floor(epochMs / 1000) });
    const utf8 = String.fromCharCode(...new TextEncoder().encode(payload));
    const base64url = btoa(utf8).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
    return `eyJhbGciOiJIUzM4NCJ9.${base64url}.firma`;
  }

  it("un token ya vencido hay que renovarlo antes de conectar el chat en vivo", () => {
    expect(isTokenExpiring(jwtExpiringAt(NOW - 1_000), NOW, MARGIN)).toBe(true);
  });

  it("un token que vence dentro del margen tambien se renueva", () => {
    expect(isTokenExpiring(jwtExpiringAt(NOW + 10_000), NOW, MARGIN)).toBe(true);
  });

  it("un token con vida de sobra se usa tal cual", () => {
    expect(isTokenExpiring(jwtExpiringAt(NOW + 20 * 60_000), NOW, MARGIN)).toBe(false);
  });

  it("lee payloads codificados en base64url (con - y _)", () => {
    const token = jwtExpiringAt(NOW + 20 * 60_000, { email: "ñandú>>>??@clinica.mx" });
    expect(isTokenExpiring(token, NOW, MARGIN)).toBe(false);
  });

  it("sin token o con uno ilegible se intenta renovar", () => {
    expect(isTokenExpiring(null, NOW, MARGIN)).toBe(true);
    expect(isTokenExpiring("no-es-un-jwt", NOW, MARGIN)).toBe(true);
    expect(isTokenExpiring("a.bm8tanNvbg.c", NOW, MARGIN)).toBe(true);
  });
});
