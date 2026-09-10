import { FormEvent, useState } from "react";
import { IconShieldLock } from "@tabler/icons-react";
import { authApi, getFriendlyError } from "@shared/api/api";
import { Field } from "@shared/ui/Field";

export function PasswordResetScreen({ onBackToLogin }: { onBackToLogin: () => void }) {
  const token = new URLSearchParams(window.location.search).get("token") ?? "";
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const password = String(form.get("password") ?? "");
    const confirmation = String(form.get("confirmation") ?? "");
    if (!token) return setError("El enlace de recuperación es inválido.");
    if (password.length < 8) return setError("La contraseña debe tener al menos 8 caracteres.");
    if (password !== confirmation) return setError("Las contraseñas no coinciden.");

    setLoading(true);
    setError("");
    try {
      await authApi.confirmPasswordReset(token, password);
      setNotice("Tu contraseña fue actualizada. Ya puedes iniciar sesión.");
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-layout">
      <section className="auth-panel" aria-labelledby="reset-password-title">
        <div className="brand-row"><span className="brand-dot" /><span className="brand-word">Clinerya</span></div>
        <div className="auth-heading"><h1 id="reset-password-title">Nueva contraseña</h1><p>Elige una contraseña nueva para acceder a tu cuenta.</p></div>
        <form className="auth-form" onSubmit={submit}>
          <Field name="password" label="Nueva contraseña" type="password" autoComplete="new-password" required />
          <Field name="confirmation" label="Confirmar contraseña" type="password" autoComplete="new-password" required />
          {error && <p className="alert error">{error}</p>}
          {notice && <p className="alert success">{notice}</p>}
          {!notice && <button className="btn primary" disabled={loading} type="submit"><IconShieldLock size={20} />{loading ? "Procesando" : "Actualizar contraseña"}</button>}
        </form>
        <p className="auth-switch"><button type="button" className="link-btn" onClick={onBackToLogin}>Volver a iniciar sesión</button></p>
      </section>
    </main>
  );
}
