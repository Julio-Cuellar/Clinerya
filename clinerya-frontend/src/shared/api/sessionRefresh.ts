// Stub con el comportamiento actual de api.ts: se reemplaza en el GREEN.
export function shouldRefresh(path: string, status: number): boolean {
  return status === 401 && path !== "/v1/auth/refresh";
}

export function createSessionRefresher(
  doRefresh: () => Promise<string>,
  onExpired: () => void
): () => Promise<string> {
  void onExpired;
  return () => doRefresh();
}
