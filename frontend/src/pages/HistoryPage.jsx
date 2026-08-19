import { useEffect, useMemo, useRef, useState } from "react";
import { apiFetch } from "../api/apiFetch";

const API_BASE_URL = "";
const STATUSES = [
  { value: "DRAFT", label: "Draft" },
  { value: "DOING", label: "Doing" },
  { value: "REVIEW", label: "Review" },
  { value: "DONE", label: "Done" },
];

function formatDate(date) {
  if (!date) return "";
  return new Intl.DateTimeFormat("en-GB", {
    day: "2-digit", month: "short", year: "numeric", timeZone: "Asia/Kuala_Lumpur",
  }).format(new Date(`${date}T00:00:00+08:00`));
}

function formatDateTime(value) {
  if (!value) return "Unknown";
  return new Intl.DateTimeFormat("en-GB", {
    dateStyle: "medium", timeStyle: "short", timeZone: "Asia/Kuala_Lumpur",
  }).format(new Date(value));
}

function HistoricalTaskCard({ task }) {
  return (
    <div className="task-card historical-task-card">
      <h3>{task.title}</h3>
      {task.boardName && <small className="task-board-name">{task.boardName}</small>}
      {task.description && <p>{task.description}</p>}
      <div className="task-meta">
        <span>{task.priority || "No priority"} · Workload {task.workload ?? "—"}</span>
        {task.assigneeName && <span>{task.assigneeName}</span>}
      </div>
      {task.dueDate && <small>Due: {task.dueDate}</small>}
      {task.createdByName && <small>Created by {task.createdByName}</small>}
    </div>
  );
}

function HistoryPage({ user, users, departments }) {
  const today = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Kuala_Lumpur" }).format(new Date());
  const [dates, setDates] = useState([]);
  const [date, setDate] = useState(today);
  const [snapshotType, setSnapshotType] = useState("END_OF_DAY");
  const [selectedUserId, setSelectedUserId] = useState(user?.role === "MANAGER" ? String(user.userId) : "");
  const [selectedDepartmentId, setSelectedDepartmentId] = useState(user?.role === "ADMIN" ? "" : String(user?.departmentId ?? ""));
  const [tasks, setTasks] = useState([]);
  const [status, setStatus] = useState("loading-dates");
  const [error, setError] = useState("");
  const requestId = useRef(0);

  useEffect(() => {
    let cancelled = false;
    async function loadDates() {
      setStatus("loading-dates");
      try {
        const response = await apiFetch(`${API_BASE_URL}/api/history/dates`);
        if (!response.ok) throw new Error(`History dates failed (${response.status})`);
        const data = await response.json();
        if (!cancelled) { setDates(data); setStatus("ready"); }
      } catch (loadError) {
        if (!cancelled) { setError("Unable to load history dates."); setStatus("error"); }
        console.error(loadError);
      }
    }
    loadDates();
    return () => { cancelled = true; };
  }, []);

  const availableDate = dates.find((item) => item.date === date);
  const filteredUsers = useMemo(() => users.filter((candidate) =>
    !selectedDepartmentId || String(candidate.departmentId) === String(selectedDepartmentId)
  ), [users, selectedDepartmentId]);

  useEffect(() => {
    if (!date || dates.length === 0) return undefined;
    const currentRequest = ++requestId.current;
    const controller = new AbortController();
    async function loadHistory() {
      setTasks([]);
      setError("");
      setStatus("loading-history");
      const type = encodeURIComponent(snapshotType);
      const path = selectedUserId
        ? `/api/history/${date}/users/${selectedUserId}?type=${type}`
        : `/api/history/${date}?type=${type}`;
      try {
        const response = await apiFetch(`${API_BASE_URL}${path}`, { signal: controller.signal });
        if (currentRequest !== requestId.current) return;
        if (response.status === 403) {
          setError("You do not have permission to view this history.");
          setStatus("error");
          return;
        }
        if (response.status === 404) {
          setStatus("missing");
          return;
        }
        if (!response.ok) throw new Error(`History request failed (${response.status})`);
        setTasks(await response.json());
        setStatus("ready");
      } catch (loadError) {
        if (loadError.name === "AbortError" || currentRequest !== requestId.current) return;
        setError("Unable to load this historical Kanban.");
        setStatus("error");
      }
    }
    loadHistory();
    return () => controller.abort();
  }, [date, snapshotType, selectedUserId, dates]);

  const columns = useMemo(() => Object.fromEntries(STATUSES.map((column) => [
    column.value, tasks.filter((task) => task.status === column.value)
      .sort((a, b) => (a.position ?? 0) - (b.position ?? 0)),
  ])), [tasks]);
  const metadataTask = tasks[0];
  const isToday = date === today;
  const typeLabel = snapshotType === "START_OF_DAY" ? "Start of Day" : "End of Day";

  function typeAvailable(type) {
    return type === "START_OF_DAY" ? availableDate?.hasStartOfDay : availableDate?.hasEndOfDay;
  }

  return (
    <>
      <div className="history-heading">
        <div>
          <h1>History</h1>
          <p>Frozen daily snapshots · <span className="read-only-badge">Read-only history</span></p>
        </div>
      </div>

      <div className="board-toolbar history-toolbar">
        {user?.role === "ADMIN" && (
          <div className="toolbar-field">
            <label htmlFor="history-department">Department</label>
            <select id="history-department" value={selectedDepartmentId} onChange={(event) => { setSelectedDepartmentId(event.target.value); setSelectedUserId(""); }}>
              <option value="">All departments</option>
              {departments.map((department) => <option key={department.id} value={department.id}>{department.name}</option>)}
            </select>
          </div>
        )}
        {user?.role !== "STAFF" && (
          <div className="toolbar-field">
            <label htmlFor="history-user">Employee</label>
            <select id="history-user" value={selectedUserId} onChange={(event) => setSelectedUserId(event.target.value)}>
              <option value="">Overall</option>
              {filteredUsers.map((candidate) => <option key={candidate.id} value={candidate.id}>{candidate.name || candidate.email}</option>)}
            </select>
          </div>
        )}
        <div className="toolbar-field">
          <label htmlFor="history-date">Date</label>
          <input id="history-date" type="date" value={date} list="history-dates" onChange={(event) => setDate(event.target.value)} />
          <datalist id="history-dates">{dates.map((item) => <option key={item.date} value={item.date} />)}</datalist>
        </div>
        <div className="toolbar-field">
          <span className="toolbar-label">Snapshot</span>
          <div className="history-type-toggle">
            <button type="button" className={snapshotType === "START_OF_DAY" ? "active" : ""} disabled={!typeAvailable("START_OF_DAY")} onClick={() => setSnapshotType("START_OF_DAY")}>Start of Day</button>
            <button type="button" className={snapshotType === "END_OF_DAY" ? "active" : ""} disabled={!typeAvailable("END_OF_DAY")} onClick={() => setSnapshotType("END_OF_DAY")}>End of Day</button>
          </div>
        </div>
      </div>

      {isToday && <p className="history-notice">Today's live work is available in My Kanban. This page shows captured snapshots only.</p>}
      {status === "loading-dates" && <div className="empty-state"><p>Loading history...</p></div>}
      {status === "loading-history" && <div className="empty-state"><p>Loading historical Kanban...</p></div>}
      {error && <p className="form-error" role="alert">{error}</p>}
      {status === "ready" && dates.length === 0 && <div className="empty-state"><p>No history has been captured yet.</p></div>}
      {status === "missing" && <div className="empty-state"><p>No {typeLabel} snapshot is available for this date.</p></div>}
      {status === "ready" && dates.length > 0 && !availableDate && <div className="empty-state"><p>No historical snapshot is available for this date.</p></div>}
      {status === "ready" && availableDate && (
        <>
          <div className="history-metadata">
            <strong>{formatDate(date)} — {typeLabel}</strong>
            {metadataTask?.recovered && <span>Recovered snapshot</span>}
            {metadataTask?.capturedAt && <span>Captured at {formatDateTime(metadataTask.capturedAt)}</span>}
            <span>{tasks.length} {tasks.length === 1 ? "task" : "tasks"}</span>
          </div>
          {tasks.length === 0 ? <div className="empty-state"><p>No tasks were recorded in this snapshot.</p></div> : (
            <div className="kanban-board historical-kanban">
              {STATUSES.map((column) => <div className="kanban-column" key={column.value}>
                <div className="column-header"><h2>{column.label}</h2><span className="history-count">{columns[column.value].length}</span></div>
                <div className="task-list">{columns[column.value].map((task) => <HistoricalTaskCard task={task} key={`${task.taskId}-${task.position}`} />)}</div>
                {columns[column.value].length === 0 && <p className="column-empty">No tasks recorded.</p>}
              </div>)}
            </div>
          )}
        </>
      )}
    </>
  );
}

export default HistoryPage;
