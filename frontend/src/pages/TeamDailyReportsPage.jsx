import { useEffect, useState } from "react";
import { apiFetch } from "../api/apiFetch";
import WeeklyReportPage from "./WeeklyReportPage";
import ReportCalendar from "../components/ReportCalendar";
import { ReportSkeleton } from "../components/ReportSkeleton";

const API = "";
const today = () => new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Kuala_Lumpur" }).format(new Date());
const niceDate = (value) => new Date(`${value}T00:00:00`).toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
const label = (s) => s === "NOT_STARTED" ? "Not started" : s === "SUBMITTED" ? "Submitted" : "Draft";

export default function TeamDailyReportsPage({ user, departments, onOpenReport, onOpenOwnReport }) {
  const isAdmin = user.role === "ADMIN";
  const [date, setDate] = useState(today());
  const [departmentId, setDepartmentId] = useState(user.departmentId || departments[0]?.id || "");
  const [data, setData] = useState(null);
  const [state, setState] = useState("loading");
  const [error, setError] = useState("");
  const [exporting, setExporting] = useState(false);
  const [mode, setMode] = useState("daily");
  const [availability, setAvailability] = useState({});
  const [calendarMonth, setCalendarMonth] = useState(date.slice(0, 7));
  useEffect(() => { let cancelled = false; apiFetch(`${API}/api/daily-reports/availability?month=${calendarMonth}${isAdmin ? `&departmentId=${departmentId}` : ""}`).then((r) => r.ok ? r.json() : []).then((rows) => { if (!cancelled) setAvailability(Object.fromEntries(rows.map((row) => [row.date, row.hasReports]))); }).catch(() => { if (!cancelled) setAvailability({}); }); return () => { cancelled = true; }; }, [calendarMonth, departmentId, isAdmin]);
  useEffect(() => {
    if (!departmentId) return;
    let cancelled = false;
    apiFetch(`${API}/api/daily-reports/team?date=${date}${isAdmin ? `&departmentId=${departmentId}` : ""}`)
      .then(async (r) => { if (!r.ok) throw new Error(r.status === 403 ? "You do not have access to this department." : "Unable to load team reports."); return r.json(); })
      .then((value) => { if (!cancelled) { setData(value); setState("ready"); } })
      .catch((e) => { if (!cancelled) { setError(e.message); setState("error"); } });
    return () => { cancelled = true; };
  }, [date, departmentId, isAdmin]);
  if (mode === "weekly") return <><div className="report-tabs" role="tablist"><button role="tab" aria-selected="false" onClick={() => setMode("daily")}>Daily</button><button role="tab" aria-selected="true" className="is-active">Weekly</button></div><WeeklyReportPage user={user} departments={departments} initialDate={date} onBackToDaily={() => setMode("daily")} /></>;
  if (state === "loading") return <><div className="report-tabs" role="tablist"><button role="tab" aria-selected="true" className="is-active">Daily</button><button role="tab" aria-selected="false" onClick={() => setMode("weekly")}>Weekly</button></div><ReportSkeleton /></>;
  if (state === "error") return <section className="dashboard-page"><p className="dashboard-error">{error}</p></section>;
  async function exportPdf() {
    setExporting(true); setError("");
    try {
      const response = await apiFetch(`${API}/api/daily-reports/team/pdf?date=${date}${isAdmin ? `&departmentId=${departmentId}` : ""}`);
      if (!response.ok) throw new Error(response.status === 403 ? "You do not have access to this department." : "Unable to generate team PDF.");
      const blob = await response.blob(); const url = URL.createObjectURL(blob); const link = document.createElement("a");
      link.href = url; link.download = `FlowOps-${data.departmentName}-${date}-Team-Daily-Report.pdf`; link.click(); URL.revokeObjectURL(url);
    } catch (e) { setError(e.message); } finally { setExporting(false); }
  }
  return <section className="dashboard-page team-reports-page">
    <div className="report-tabs" role="tablist"><button role="tab" aria-selected="true" className="is-active">Daily</button><button role="tab" aria-selected="false" onClick={() => setMode("weekly")}>Weekly</button></div><div className="report-heading"><div><p className="eyebrow">FlowOps</p><h1>Daily Reports</h1><p>{niceDate(data.date)} · {data.departmentName} Department</p></div><div className="report-actions"><ReportCalendar value={date} onChange={setDate} availability={availability} onMonthChange={setCalendarMonth} />{isAdmin && <label className="date-control">Department <select value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>{departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}</select></label>}<button className="secondary-button" onClick={exportPdf} disabled={exporting || !data.reports.some((report) => report.status !== "NOT_STARTED")}>{exporting ? "Generating…" : "Export Team PDF"}</button><button className="secondary-button" onClick={onOpenOwnReport}>My Daily Report</button></div></div>
    {!data.reports.some((report) => report.status !== "NOT_STARTED") && <p className="report-helper">No report data available for this date.</p>}
    {error && <p className="report-message">{error}</p>}
    <div className="team-report-summary"><strong>{data.submittedCount} / {data.reports.length} submitted</strong><span className="report-status submitted">Submitted {data.submittedCount}</span><span className="report-status draft">Draft {data.draftCount}</span><span className="report-status not_started">Not started {data.notStartedCount}</span></div>
    <div className="team-report-list">{data.reports.map((report) => <ReportCard key={report.userId} report={report} onOpen={() => onOpenReport(report.userId, date)} />)}</div>
    {!data.reports.length && <p className="dashboard-empty">No staff found in this department.</p>}
  </section>;
}
function ReportCard({ report, onOpen }) { const [expanded, setExpanded] = useState(report.status === "SUBMITTED"); return <article className={`team-report-card ${report.status.toLowerCase()}`}><div className="team-report-card-header"><div><h2>{report.userName}</h2><span className={`report-status ${report.status.toLowerCase()}`}>{label(report.status)}</span></div><button className="text-button" onClick={() => setExpanded((v) => !v)}>{expanded ? "Collapse" : "Expand"}</button></div>{expanded && <><div className="team-report-copy"><Block title="Work Summary" value={report.workSummary} /><Block title="Blockers" value={report.blockers} /><Block title="Next Plan" value={report.nextDayPlan} /></div><div className="team-report-metrics"><span>Done <b>{report.completedCount}</b></span><span>Review <b>{report.reviewCount}</b></span><span>Doing <b>{report.doingCount}</b></span><span>Workload <b>{report.activeWorkload}</b></span></div>{report.submittedAt && <small>Submitted {new Date(report.submittedAt).toLocaleString()}</small>}<button className="secondary-button" onClick={onOpen}>View full report</button></>}{!expanded && <p className="dashboard-empty">{report.status === "NOT_STARTED" ? "No report submitted yet." : "Draft saved."}</p>}</article>; }
function Block({ title, value }) { return <div><h3>{title}</h3><p>{value || "No update provided."}</p></div>; }
