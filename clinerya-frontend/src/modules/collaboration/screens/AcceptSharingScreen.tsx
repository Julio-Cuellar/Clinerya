import { useEffect, useState } from "react";
import { IconCheck, IconShieldLock, IconX } from "@tabler/icons-react";
import { collaborationApi, getFriendlyError, sessionStore } from "@shared/api/api";

export function AcceptSharingScreen({ onBackToLogin }: { onBackToLogin: () => void }) {
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [accepted, setAccepted] = useState(false);

  const grantId = new URLSearchParams(window.location.search).get("grantId") || "";
  const isLoggedIn = Boolean(sessionStore.getAccessToken());

  useEffect(() => {
    if (!isLoggedIn) {
      setLoading(false);
      return;
    }
    if (!grantId) {
      setError("Falta el identificador de acceso (grantId) en el enlace.");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError("");
    setNotice("");

    collaborationApi.accept(grantId)
      .then(() => {
        setAccepted(true);
        setNotice("Has aceptado el acceso al expediente compartido correctamente.");
      })
      .catch((caught) => {
        setError(getFriendlyError(caught));
      })
      .finally(() => {
        setLoading(false);
      });
  }, [grantId, isLoggedIn]);

  return (
    <main className="auth-layout">
      <section className="auth-panel" aria-labelledby="auth-title">
        <div className="brand-row">
          <span className="brand-dot" />
          <span className="brand-word">Clinerya</span>
        </div>
        <div className="auth-heading">
          <h1 id="auth-title">Acceso a Expediente Compartido</h1>
          <p>Acepta la invitación para colaborar y consultar el expediente desde tu cuenta.</p>
        </div>

        {!grantId ? (
          <div className="alert error">
            El enlace de invitación es inválido. Por favor, solicita un nuevo enlace al médico emisor.
          </div>
        ) : !isLoggedIn ? (
          <div className="auth-form" style={{ textAlign: "center", display: "flex", flexDirection: "column", gap: "20px" }}>
            <p style={{ fontSize: "14px", color: "var(--color-text-2)", lineHeight: "1.5" }}>
              Has recibido una invitación de expediente compartido. Para poder visualizarlo y colaborar, necesitas tener una cuenta activa en Clinerya.
            </p>
            <div className="alert warning" style={{ margin: 0 }}>
              Debes iniciar sesión o registrar una cuenta con tu correo para aceptar la invitación.
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              <button className="btn primary" type="button" onClick={onBackToLogin} style={{ width: "100%" }}>
                <IconShieldLock size={20} aria-hidden="true" style={{ marginRight: "6px" }} />
                Ya tengo cuenta: Iniciar sesión
              </button>
              <button className="btn secondary" type="button" onClick={() => window.location.assign("/?register=true")} style={{ width: "100%" }}>
                No tengo cuenta: Registrarme en Clinerya
              </button>
            </div>
          </div>
        ) : (
          <div className="auth-form" style={{ textAlign: "center", display: "flex", flexDirection: "column", gap: "20px" }}>
            {loading && <p>Procesando la aceptación de la invitación...</p>}
            {error && (
              <div className="alert error" style={{ display: "flex", alignItems: "center", gap: "8px", margin: 0 }}>
                <IconX size={20} />
                <span>{error}</span>
              </div>
            )}
            {accepted && (
              <div className="alert success" style={{ display: "flex", alignItems: "center", gap: "8px", margin: 0 }}>
                <IconCheck size={20} />
                <span>{notice}</span>
              </div>
            )}

            {!loading && (
              <button className="btn primary" type="button" onClick={onBackToLogin} style={{ width: "100%" }}>
                Ir a mi panel de Clinerya
              </button>
            )}
          </div>
        )}

        <p className="auth-switch" style={{ marginTop: "20px" }}>
          ¿Tienes dudas sobre el expediente? Consulta con el médico emisor de la clínica de origen.
        </p>
      </section>
    </main>
  );
}
