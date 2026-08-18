import { useEffect, useState } from "react";
import NotificationItem from "./NotificationItem";

function NotificationPanel({ notifications, loading, onOpen, onMarkAllRead, onClear, onClearAll }) {
  const [filter, setFilter] = useState("all");
  const [now, setNow] = useState(null);
  useEffect(() => {
    const initial = window.setTimeout(() => setNow(Date.now()), 0);
    const interval = window.setInterval(() => setNow(Date.now()), 60000);
    return () => { window.clearTimeout(initial); window.clearInterval(interval); };
  }, []);
  const visible = filter === "unread" ? notifications.filter((item) => !item.read) : notifications;
  const hasUnread = notifications.some((item) => !item.read);

  return (
    <section className="notification-panel" role="dialog" aria-label="Notifications">
      <div className="notification-panel-header">
        <h2>Notifications</h2>
        <div className="notification-tabs" role="tablist" aria-label="Notification filter">
          <button type="button" role="tab" aria-selected={filter === "all"} className={filter === "all" ? "is-active" : ""} onClick={() => setFilter("all")}>All</button>
          <button type="button" role="tab" aria-selected={filter === "unread"} className={filter === "unread" ? "is-active" : ""} onClick={() => setFilter("unread")}>Unread</button>
        </div>
      </div>
      <div className="notification-actions">
        <button type="button" onClick={onMarkAllRead} disabled={!hasUnread}>Mark all read</button>
        <button type="button" onClick={onClearAll} disabled={notifications.length === 0}>Clear all</button>
      </div>
      <div className="notification-list">
        {loading && notifications.length === 0 ? <p className="notification-empty">Loading…</p> : visible.length === 0 ? <p className="notification-empty">{filter === "unread" ? "No unread notifications" : "No notifications"}</p> : visible.map((notification) => (
          <NotificationItem key={notification.id} notification={notification} now={now} onOpen={onOpen} onClear={onClear} />
        ))}
      </div>
    </section>
  );
}

export default NotificationPanel;
