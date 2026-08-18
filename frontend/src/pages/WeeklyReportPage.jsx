import { useEffect, useState } from "react";
import { apiFetch } from "../api/apiFetch";
import ReportCalendar from "../components/ReportCalendar";
import { ReportSkeleton } from "../components/ReportSkeleton";

const API = "http://localhost:8080";
const zone = "Asia/Kuala_Lumpur";
const today = () => new Intl.DateTimeFormat("en-CA", { timeZone: zone }).format(new Date());
const monday = (value) => { const d = new Date(`${value}T00:00:00`); const day = d.getDay() || 7; d.setDate(d.getDate() - day + 1); return d.toISOString().slice(0, 10); };
const shift = (value, amount) => { const d = new Date(`${value}T00:00:00`); d.setDate(d.getDate() + amount); return d.toISOString().slice(0, 10); };
const compact = (value) => (value || "No update provided.").replace(/\s+/g, " ").trim();

export default function WeeklyReportPage({ user, departments, initialDate }) {
  const isAdmin = user.role === "ADMIN";
  const [weekStart, setWeekStart] = useState(monday(initialDate || today()));
  const [departmentId, setDepartmentId] = useState(user.departmentId || departments[0]?.id || "");
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [exporting, setExporting] = useState(false);
  const [availability, setAvailability] = useState({});
  const [calendarMonth, setCalendarMonth] = useState((initialDate || today()).slice(0, 7));
  const query = `${API}/api/daily-reports/weekly?weekStart=${weekStart}${isAdmin ? `&departmentId=${departmentId}` : ""}`;
  useEffect(() => { if (!departmentId) return; let cancelled = false; apiFetch(query).then(async (r) => { if (!r.ok) throw new Error(r.status === 403 ? "You do not have access to this department." : "Unable to load weekly reports."); return r.json(); }).then((v) => { if (!cancelled) { setError(""); setData(v); } }).catch((e) => !cancelled && setError(e.message)); return () => { cancelled = true; }; }, [query, departmentId]);
  useEffect(() => { let cancelled = false; apiFetch(`${API}/api/daily-reports/availability?month=${calendarMonth}${isAdmin ? `&departmentId=${departmentId}` : ""}`).then((r) => r.ok ? r.json() : []).then((rows) => { if (!cancelled) setAvailability(Object.fromEntries(rows.map((row) => [row.date, row.hasReports]))); }).catch(() => { if (!cancelled) setAvailability({}); }); return () => { cancelled = true; }; }, [calendarMonth, departmentId, isAdmin]);
  async function exportPdf() { setExporting(true); setError(""); try { const r = await apiFetch(`${API}/api/daily-reports/weekly/pdf?weekStart=${weekStart}${isAdmin ? `&departmentId=${departmentId}` : ""}`); if (!r.ok) throw new Error("Unable to generate weekly PDF."); const url = URL.createObjectURL(await r.blob()); const a = document.createElement("a"); a.href = url; a.download = `Kovax-FlowOps-${data.departmentName}-${weekStart}-Weekly-Report.pdf`; a.click(); URL.revokeObjectURL(url); } catch (e) { setError(e.message); } finally { setExporting(false); } }
  const overview = data?.overview;
  if (error) return <section className="dashboard-page"><p className="dashboard-error">{error}</p></section>;
  if (!data) return <ReportSkeleton weekly />;
  return <section className="dashboard-page team-reports-page">
    <div className="report-heading"><div><p className="eyebrow">Kovax FlowOps</p><h1>Weekly Report</h1><p>{formatRange(data.weekStart, data.weekEnd)}<br />{data.departmentName} Department</p></div><div className="report-actions"><ReportCalendar value={weekStart} weekly onChange={setWeekStart} availability={availability} onMonthChange={setCalendarMonth} />{isAdmin && <label className="date-control">Department <select value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>{departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}</select></label>}<button className="secondary-button" aria-label="Previous week" onClick={() => setWeekStart(shift(weekStart, -7))}>Previous Week</button><button className="secondary-button" aria-label="Next week" disabled={weekStart >= monday(today())} onClick={() => setWeekStart(shift(weekStart, 7))}>Next Week</button><button className="secondary-button" onClick={exportPdf} disabled={exporting || overview.dailyReportsSubmitted + overview.draftReports === 0}>{exporting ? "Generating…" : "Export Weekly PDF"}</button></div></div>
    {overview.dailyReportsSubmitted + overview.draftReports === 0 && <p className="report-helper">No reports available for {formatRange(data.weekStart, data.weekEnd)}.</p>}
    <div className="weekly-overview"><h2>Weekly Overview</h2><div className="weekly-kpis"><Kpi label="Staff" value={overview.staff} /><Kpi label="Submitted" value={overview.dailyReportsSubmitted} /><Kpi label="Draft" value={overview.draftReports} /><Kpi label="Not started" value={overview.notStarted} /><Kpi label="Tasks completed" value={overview.tasksCompleted} /><Kpi label="Moved to review" value={overview.tasksMovedToReview} /><Kpi label="Avg active workload" value={overview.averageActiveWorkload ?? "—"} /><Kpi label="Week workload" value={`${overview.weekStartWorkload ?? "—"} → ${overview.weekEndWorkload ?? "—"}${overview.weekEndCurrent ? " current" : ""}`} /></div></div>
    <div className="weekly-days">{data.days.map((day) => <Day key={day.date} day={day} />)}</div>
  </section>;
}
function Kpi({ label, value }) { return <div><span>{label}</span><strong>{value}</strong></div>; }
function Day({ day }) { return <article className="weekly-day"><h2>{new Date(`${day.date}T00:00:00`).toLocaleDateString(undefined, { weekday: "long", day: "numeric", month: "short" }).toUpperCase()}</h2><div className="weekly-staff-list">{day.staff.map((s) => <div className={`weekly-staff ${s.status.toLowerCase()}`} key={s.userId}><div className="weekly-staff-heading"><strong>{s.userName}</strong><span className={`report-status ${s.status.toLowerCase()}`}>{s.status === "NOT_STARTED" ? "Not started" : s.status === "SUBMITTED" ? "Submitted" : "Draft"}</span></div>{s.status === "NOT_STARTED" ? <p className="weekly-muted">No report submitted.</p> : <><div className="weekly-copy"><div><b>Summary</b><p>{compact(s.workSummary)}</p></div><div><b>Blocker</b><p>{compact(s.blockers)}</p></div><div><b>Next</b><p>{compact(s.nextDayPlan)}</p></div></div><div className="weekly-tasks">{s.tasks.map((t) => <span key={t.taskId}>[{t.status}] {t.title}</span>)}</div><small>{s.completedCount} completed · {s.reviewCount} moved to review{s.start && s.end ? ` · Workload ${s.start.workload} → ${s.end.workload}` : ""}</small></>}</div>)}</div></article>; }
function formatRange(start, end) { const s = new Date(`${start}T00:00:00`), e = new Date(`${end}T00:00:00`); const sm = s.toLocaleDateString(undefined, { month: "short" }), em = e.toLocaleDateString(undefined, { month: "short" }); return `${s.getDate()} ${sm} – ${e.getDate()} ${em} ${e.getFullYear()}`; }
