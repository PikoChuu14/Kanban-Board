import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { apiFetch } from "../api/apiFetch";
import { useAuth } from "./AuthContext";

const NotificationContext = createContext(null);
const API_BASE_URL = "";

export function NotificationProvider({ children }) {
  const { isAuthenticated, user } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const fetchSequence = useRef(0);
  const mutationVersion = useRef(0);
  // A refresh can race with the PATCH that marks a notification as read. Keep
  // the local read decision authoritative until the server has caught up.
  const readOverrides = useRef(new Set());

  const fetchNotifications = useCallback(async () => {
    if (!isAuthenticated) return;
    const requestSequence = ++fetchSequence.current;
    const versionAtStart = mutationVersion.current;
    setLoading(true);
    try {
      const response = await apiFetch(`${API_BASE_URL}/api/notifications?limit=30`);
      if (!response.ok) throw new Error(`Notifications request failed (${response.status}).`);
      const data = await response.json();
      // Opening the panel and the 60-second poll can overlap a read/clear
      // mutation. Never let a response that began before that mutation put
      // stale unread notifications back into local state.
      if (requestSequence === fetchSequence.current && versionAtStart === mutationVersion.current) {
        data.forEach((item) => {
          if (item.read) readOverrides.current.delete(item.id);
        });
        setNotifications(data.map((item) => {
          if (readOverrides.current.has(item.id)) return { ...item, read: true };
          return item;
        }));
      }
    } catch (error) {
      console.error("Failed to load notifications:", error);
    } finally {
      if (requestSequence === fetchSequence.current) setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (!isAuthenticated) return undefined;
    const initial = window.setTimeout(fetchNotifications, 0);
    const interval = window.setInterval(fetchNotifications, 60000);
    return () => {
      window.clearTimeout(initial);
      window.clearInterval(interval);
    };
  }, [isAuthenticated, user?.userId, fetchNotifications]);

  const markRead = useCallback(async (id) => {
    mutationVersion.current += 1;
    readOverrides.current.add(id);
    setNotifications((items) => items.map((item) => item.id === id ? { ...item, read: true } : item));
    const response = await apiFetch(`${API_BASE_URL}/api/notifications/${id}/read`, { method: "PATCH" });
    if (!response.ok) {
      readOverrides.current.delete(id);
      await fetchNotifications();
      return false;
    }
    const persisted = await response.json();
    setNotifications((items) => items.map((item) => item.id === id ? persisted : item));
    return true;
  }, [fetchNotifications]);

  const markAllRead = useCallback(async () => {
    mutationVersion.current += 1;
    setNotifications((items) => items.map((item) => ({ ...item, read: true })));
    const response = await apiFetch(`${API_BASE_URL}/api/notifications/read-all`, { method: "PATCH" });
    if (!response.ok) fetchNotifications();
  }, [fetchNotifications]);

  const clearNotification = useCallback(async (id) => {
    mutationVersion.current += 1;
    setNotifications((items) => items.filter((item) => item.id !== id));
    const response = await apiFetch(`${API_BASE_URL}/api/notifications/${id}`, { method: "DELETE" });
    if (!response.ok) fetchNotifications();
  }, [fetchNotifications]);

  const clearAll = useCallback(async () => {
    mutationVersion.current += 1;
    setNotifications([]);
    const response = await apiFetch(`${API_BASE_URL}/api/notifications`, { method: "DELETE" });
    if (!response.ok) fetchNotifications();
  }, [fetchNotifications]);

  const value = useMemo(() => ({
    notifications,
    unreadCount: notifications.filter((item) => !item.read).length,
    loading,
    fetchNotifications,
    markRead,
    markAllRead,
    clearNotification,
    clearAll,
  }), [notifications, loading, fetchNotifications, markRead, markAllRead, clearNotification, clearAll]);

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useNotifications() { return useContext(NotificationContext); }
