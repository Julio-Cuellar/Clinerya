import { FormEvent, useState } from "react";
import { IconShieldLock } from "@tabler/icons-react";
import { Field } from "@shared/ui/Field";
import { getFriendlyError, staffApi } from "@shared/api/api";

export function ConfirmStaffScreen({ onBackToLogin }: { onBackToLogin: () => void }) {
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  const token = new URLSearchParams(window.location.search).get("token") || "";

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!token) {
      setError("Falta el token de invitación en la URL.");
      return;
    }
    
    setLoading(true);
    setError("");
    setNotice("");
    const form = new FormData(event.currentTarget);
    const value = (name: string) => String(form.get(name) ?? "").trim();

    const fullName = value("fullName");
    const password = value("password");
    const confirmPassword = value("confirmPassword");

    if (password !== confirmPassword) {
      setError("Las contraseñas no coinciden.");
      setLoading(false);
      return;
    }

    try {
      await staffApi.registerStaff({ token, fullName, password });
      setNotice("Registro completado exitosamente. Ya puedes iniciar sesión en Clinerya.");
      setTimeout(() => {
        onBackToLogin();
      }, 3000);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-layout">
      <section className="auth-panel" aria-labelledby="auth-title">
        <div className="brand-row">
          <span className="brand-dot" />
          <span className="brand-word">Clinerya</span>
        </div>
        <div className="auth-heading">
          <h1 id="auth-title">Confirmar Registro</h1>
          <p>Completa tus datos personales para activar tu cuenta de personal de la clínica.</p>
        </div>

        {!token ? (
          <div className="alert error">
            El enlace de invitación es inválido o no incluye el token necesario. 
            Por favor, solicita un nuevo enlace a tu administrador.
          </div>
        ) : (
          <form className="auth-form" onSubmit={submit}>
            <Field name="fullName" label="Nombre completo" autoComplete="name" required />
            <Field name="password" label="Contraseña" type="password" autoComplete="new-password" required />
            <Field name="confirmPassword" label="Confirmar contraseña" type="password" autoComplete="new-password" required />

            {error && <p className="alert error">{error}</p>}
            {notice && <p className="alert success">{notice}</p>}
            
            <button className="btn primary" disabled={loading || !!notice} type="submit">
              <IconShieldLock size={20} aria-hidden="true" />
              {loading ? "Procesando..." : "Confirmar registro"}
            </button>
          </form>
        )}

        <p className="auth-switch" style={{ marginTop: "20px" }}>
          ¿Ya tienes cuenta?{" "}
          <button type="button" className="link-btn" onClick={onBackToLogin}>
            Ir al inicio de sesión
          </button>
        </p>
      </section>
    </main>
  );
}
