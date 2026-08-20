import { useEffect, useState } from "react";

function ConnectionStatus() {
  const [connected, setConnected] = useState(navigator.onLine);

  useEffect(() => {
    let cancelled = false;
    const check = async () => {
      if (!navigator.onLine) return setConnected(false);
      try {
        const response = await fetch("/api/health", { cache: "no-store" });
        if (!cancelled) setConnected(response.ok);
      } catch {
        if (!cancelled) setConnected(false);
      }
    };
    const online = () => check();
    const offline = () => setConnected(false);
    window.addEventListener("online", online);
    window.addEventListener("offline", offline);
    check();
    const interval = window.setInterval(check, 30000);
    return () => {
      cancelled = true;
      window.clearInterval(interval);
      window.removeEventListener("online", online);
      window.removeEventListener("offline", offline);
    };
  }, []);

  return <div className={`connection-status ${connected ? "is-connected" : "is-offline"}`} role="status"><i />{connected ? "Connected" : "Offline / Reconnecting"}</div>;
}

export default ConnectionStatus;
