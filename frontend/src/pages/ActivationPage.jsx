import { useMemo, useState } from "react";
import { apiFetch } from "../api/apiFetch";

export default function ActivationPage() {
  const token = useMemo(() => new URLSearchParams(window.location.search).get("token") || "", []);
  const [password, setPassword] = useState(""); const [confirm, setConfirm] = useState("");
  const [message, setMessage] = useState(""); const [submitting, setSubmitting] = useState(false); const [complete, setComplete] = useState(false);
  async function submit(event) { event.preventDefault(); setMessage("");
    if (!token) return setMessage("This activation link is missing its token.");
    if (password.length < 8) return setMessage("Use at least 8 characters.");
    if (password !== confirm) return setMessage("Passwords do not match.");
    setSubmitting(true); try { const response = await apiFetch("/api/auth/activate", { method:"POST", body:JSON.stringify({ token, password }) });
      if (!response.ok) { const data=await response.json().catch(()=>({})); throw new Error(data.detail || data.message || "Activation failed. The link may be expired or already used."); }
      setComplete(true);
    } catch(error) { setMessage(error.message); } finally { setSubmitting(false); }
  }
  return <div className="activation-page"><div className="activation-card"><div className="brand-mark">K<span /></div>
    {complete ? <><h1>Account activated</h1><p>Your password is set and your account is ready.</p><a className="primary-button" href="/login">Continue to sign in</a></> : <>
      <p className="eyebrow">FlowOps</p><h1>Activate your account</h1><p>Choose a password to finish setting up your account.</p>
      <form onSubmit={submit}><label>New password<input type="password" autoComplete="new-password" value={password} onChange={e=>setPassword(e.target.value)} required minLength="8" /></label>
        <label>Confirm password<input type="password" autoComplete="new-password" value={confirm} onChange={e=>setConfirm(e.target.value)} required minLength="8" /></label>
        {message && <div className="admin-message error">{message}</div>}<button className="primary-button" disabled={submitting}>{submitting?"Activating…":"Activate account"}</button></form></>}
  </div></div>;
}
