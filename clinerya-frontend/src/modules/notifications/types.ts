export interface NotificationResponse {
  id: string;
  sourceModule: string;
  sourceKey: string;
  severity: "CRITICAL" | "WARNING" | "INFO" | "SUCCESS";
  title: string;
  message: string;
  actionPath?: string | null;
  createdAt: string;
  expiresAt?: string | null;
  readAt?: string | null;
}

export interface NotificationListResponse {
  items: NotificationResponse[];
  unreadCount: number;
}
