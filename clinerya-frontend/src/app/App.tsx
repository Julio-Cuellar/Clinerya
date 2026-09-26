import { useEffect, useState } from "react";
import { AppShell } from "@app/AppShell";
import { AuthScreen } from "@modules/auth/screens/AuthScreen";
import { ClinicSetupScreen } from "@modules/clinics/screens/ClinicSetupScreen";
import { ConfirmStaffScreen } from "@modules/auth/screens/ConfirmStaffScreen";
import { PasswordResetScreen } from "@modules/auth/screens/PasswordResetScreen";
import { AcceptSharingScreen } from "@modules/collaboration/screens/AcceptSharingScreen";
import { ShareViewScreen } from "@modules/collaboration/screens/ShareViewScreen";
import { authApi, sessionStore } from "@shared/api/api";
import type { UserProfile } from "@modules/auth/types";
import type { Theme } from "@modules/settings/themeTypes";

export default function App() {
  const [user, setUser] = useState<UserProfile | null>(() => sessionStore.getUser());
  const [needsClinicSetup, setNeedsClinicSetup] = useState(false);
  const [theme, setTheme] = useState<Theme>(() => (localStorage.getItem("medicloud.theme") as Theme) ?? "light");
  const [colorPalette, setColorPalette] = useState<string>(() => localStorage.getItem("medicloud.color-palette") ?? "default");

  const isConfirmStaff = window.location.pathname.startsWith("/confirm-staff");
  const isPasswordReset = window.location.pathname.startsWith("/reset-password");
  const isAcceptSharing = window.location.pathname.startsWith("/accept-sharing");
  const isShareView = window.location.pathname.startsWith("/share-view");

  useEffect(() => {
    if (user) {
      document.documentElement.dataset.theme = theme;
      localStorage.setItem("medicloud.theme", theme);
    } else {
      document.documentElement.dataset.theme = "light";
    }
  }, [theme, user]);

  useEffect(() => {
    if (user) {
      document.documentElement.dataset.colorPalette = colorPalette;
      localStorage.setItem("medicloud.color-palette", colorPalette);
    } else {
      document.documentElement.dataset.colorPalette = "default";
    }
  }, [colorPalette, user]);

  // Si el token no pudo renovarse (cookie vencida o revocada), vuelve al login en vez de dejar
  // la pantalla abierta sin sesion.
  useEffect(() => sessionStore.onSessionExpired(() => setUser(null)), []);

  useEffect(() => {
    if (!sessionStore.getAccessToken()) return;
    authApi
      .me()
      .then((profile) => {
        sessionStore.setUser(profile);
        setUser(profile);
      })
      .catch(() => {
        sessionStore.clear();
        setUser(null);
      });
  }, []);

  if (isConfirmStaff) {
    return <ConfirmStaffScreen onBackToLogin={() => window.location.assign("/")} />;
  }

  if (isPasswordReset) {
    return <PasswordResetScreen onBackToLogin={() => window.location.assign("/")} />;
  }

  if (isAcceptSharing) {
    return <AcceptSharingScreen onBackToLogin={() => window.location.assign("/")} />;
  }

  if (isShareView) {
    return <ShareViewScreen />;
  }

  if (!user) {
    return (
      <AuthScreen
        onAuthenticated={setUser}
        onVerifiedAndAuthenticated={(profile) => {
          setUser(profile);
          setNeedsClinicSetup(true);
        }}
      />
    );
  }

  if (needsClinicSetup) {
    return (
      <ClinicSetupScreen
        user={user}
        onDone={(updatedUser) => {
          setUser(updatedUser);
          setNeedsClinicSetup(false);
        }}
      />
    );
  }

  return (
    <AppShell
      user={user}
      setUser={setUser}
      theme={theme}
      setTheme={setTheme}
      colorPalette={colorPalette}
      setColorPalette={setColorPalette}
    />
  );
}
