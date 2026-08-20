import { useEffect, useState } from "react";
import { apiFetch } from "../api/apiFetch";
import InstallFlowOps from "../components/InstallFlowOps";

function ClientAccessPage() {
  const [info, setInfo] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    apiFetch("/api/admin/system/client-access")
      .then(async (response) => {
        if (!response.ok) throw new Error("Client access information is unavailable.");
        setInfo(await response.json());
      })
      .catch((requestError) => setError(requestError.message));
  }, []);

  const primaryUrl = info?.configuredBaseUrl || info?.suggestedNetworkUrl;
  const detectedUrls = info?.detectedNetworkUrls || [];
  return <section className="admin-page client-access-page">
    <header className="admin-page-heading"><div><p className="eyebrow">Settings · System</p><h1>Client Access</h1><p>Connect staff devices to this central FlowOps server.</p></div><InstallFlowOps /></header>
    {error && <div className="admin-message error">{error}</div>}
    <div className="client-access-grid">
      <article className="client-access-card"><h2>FlowOps Server</h2><dl>
        <div><dt>Local</dt><dd><a href={info?.localUrl}>{info?.localUrl || "Loading…"}</a></dd></div>
        <div><dt>Configured company address</dt><dd>{info?.configuredBaseUrl ? <a href={info.configuredBaseUrl}>{info.configuredBaseUrl}</a> : <span className="not-configured">Not configured</span>}</dd></div>
        <div><dt>Detected network addresses</dt><dd>{detectedUrls.length > 0 ? <ul className="detected-addresses">{detectedUrls.map((url) => <li key={url}><a href={url}>{url}</a>{url === info?.suggestedNetworkUrl && <small>Temporary suggestion</small>}</li>)}</ul> : "No suitable active LAN IPv4 addresses detected."}</dd></div>
      </dl>{info?.guidance && <p className="client-access-note">{info.guidance}</p>}</article>
      <article className="client-access-card"><p className="eyebrow">Windows staff</p><h2>Install FlowOps Client</h2><p>Recommended during company LAN deployment. Ask IT for <strong>FlowOps-Client-Setup.exe</strong>, enter the server address, and let the launcher open Edge or Chrome in app mode.</p><dl><div><dt>Server</dt><dd>{primaryUrl ? <code>{primaryUrl}</code> : "Configure a stable company address first."}</dd></div></dl><p className="client-access-note">The client installs only a launcher, icon, URL configuration, and shortcuts. It does not install Java, Spring Boot, PostgreSQL, a service, backups, or server secrets.</p></article>
      <article className="client-access-card"><h2>Browser and mobile access</h2><p><strong>Windows / Android:</strong> Open the company address in a supported browser, then choose Install FlowOps or Install App when available.</p><p><strong>iPhone / iPad:</strong> Open the address in Safari, tap Share, then Add to Home Screen.</p><p className="client-access-note">All clients use the same central FlowOps HTTP/HTTPS API and database. PostgreSQL must remain private to the server.</p></article>
    </div>
    <div className="https-preparation"><strong>Stable company URL</strong><p>Configure <code>APP_BASE_URL</code> (or <code>app.base-url</code>) to a stable hostname or reserved address before rollout. Use a static/reserved server IP, internal DNS hostname, or DHCP reservation now, then move to a trusted HTTPS hostname when ready. Plain IP access remains available for testing.</p></div>
  </section>;
}

export default ClientAccessPage;
