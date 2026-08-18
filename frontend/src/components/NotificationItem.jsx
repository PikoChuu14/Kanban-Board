import { formatRelativeTime } from "../utils/formatRelativeTime";

function NotificationItem({ notification, now, onOpen, onClear }) {
  return (
    <button type="button" className={`notification-item ${notification.read ? "" : "is-unread"}`} onClick={() => onOpen(notification)}>
      <span className="notification-unread-dot" aria-hidden="true" />
      <span className="notification-item-copy">
        <strong>{notification.title}</strong>
        <span>{notification.message}</span>
        <time dateTime={notification.createdAt}>{formatRelativeTime(notification.createdAt, now)}</time>
      </span>
      <span
        role="button"
        tabIndex="0"
        className="notification-clear"
        aria-label={`Clear ${notification.title}`}
        onClick={(event) => { event.stopPropagation(); onClear(notification.id); }}
        onKeyDown={(event) => {
          if (event.key === "Enter" || event.key === " ") { event.preventDefault(); event.stopPropagation(); onClear(notification.id); }
        }}
      >×</span>
    </button>
  );
}

export default NotificationItem;
