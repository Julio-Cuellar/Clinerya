import { describe, expect, it, vi } from "vitest";
import { createSessionRefresher, shouldRefresh } from "./sessionRefresh";

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
