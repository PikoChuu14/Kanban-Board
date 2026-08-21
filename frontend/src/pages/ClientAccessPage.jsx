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

  const detectedUrls = info?.detectedNetworkUrls || [];
  return <section className="admin-page client-access-page">
    <header className="admin-page-heading"><div><p className="eyebrow">Settings · System</p><h1>Client Access</h1><p>Connect staff devices to this central FlowOps server.</p></div><InstallFlowOps /></header>
    {error && <div className="admin-message error">{error}</div>}
    <div className="client-access-grid">
      <article className="client-access-card"><h2>FlowOps Server</h2><dl>
        <div><dt>Local</dt><dd><a href={info?.localUrl}>{info?.localUrl || "Loading…"}</a></dd></div>
        <div><dt>Company address</dt><dd>{info?.companyAddressConfigured ? <span className="configuration-status configured">Configured</span> : <span className="configuration-status not-configured">Not configured</span>}</dd></div>
        <div><dt>Configured company address</dt><dd>{info?.configuredBaseUrl ? info.companyAddressUsable ? <a href={info.configuredBaseUrl}>{info.configuredBaseUrl}</a> : <code>{info.configuredBaseUrl}</code> : "—"}</dd></div>
        <div><dt>Activation link status</dt><dd>{info?.companyAddressUsable ? <span className="configuration-status configured">Usable</span> : <span className="configuration-status not-configured">Unavailable</span>}</dd></div>
        <div><dt>Activation links use</dt><dd>{info?.activationLinkBaseUrl ? <code>{info.activationLinkBaseUrl}</code> : <span className="not-configured">Unavailable until APP_BASE_URL is configured</span>}</dd></div>
        <div><dt>Detected addresses (diagnostic only)</dt><dd>{detectedUrls.length > 0 ? <ul className="detected-addresses">{detectedUrls.map((url) => <li key={url}><a href={url}>{url}</a>{url === info?.suggestedNetworkUrl && <small>Temporary test address</small>}</li>)}</ul> : "No suitable active LAN IPv4 addresses detected."}</dd></div>
      </dl>{info?.guidance && <p className="client-access-note">{info.guidance}</p>}</article>
      <article className="client-access-card"><p className="eyebrow">Windows staff</p><h2>Install FlowOps Client</h2><p>Recommended during company LAN deployment. Ask IT for <strong>FlowOps-Client-Setup.exe</strong>, enter the server address, and let the launcher open Edge or Chrome in app mode.</p><dl><div><dt>Company rollout URL</dt><dd>{info?.activationLinkBaseUrl ? <code>{info.activationLinkBaseUrl}</code> : "Configure a stable company address first."}</dd></div>{!info?.activationLinkBaseUrl && info?.suggestedNetworkUrl && <div><dt>Temporary testing URL</dt><dd><code>{info.suggestedNetworkUrl}</code></dd></div>}</dl><p className="client-access-note">The launcher URL is configured independently on each employee PC and may use a temporary address during testing. It is never rewritten from server adapter detection.</p></article>
      <article className="client-access-card"><h2>Browser and mobile access</h2><p><strong>Windows / Android:</strong> Open the company address in a supported browser, then choose Install FlowOps or Install App when available.</p><p><strong>iPhone / iPad:</strong> Open the address in Safari, tap Share, then Add to Home Screen.</p><p className="client-access-note">All clients use the same central FlowOps HTTP/HTTPS API and database. PostgreSQL must remain private to the server.</p></article>
    </div>
    <div className="https-preparation"><strong>Stable company URL</strong><p>Set <code>APP_BASE_URL</code> (or <code>app.base-url</code> in the server configuration), then restart FlowOps. Use an internal DNS hostname first, or a static/reserved server IP, and move to a trusted HTTPS hostname when ready. Detected addresses are diagnostic and never become the activation-link base automatically.</p></div>
  </section>;
}

export default ClientAccessPage;
