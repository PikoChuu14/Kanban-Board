import { useEffect, useState } from "react";
import { apiFetch } from "../api/apiFetch";

const API_BASE_URL = "http://localhost:8080";

function ManagerDashboard({ refreshKey, onViewKanban }) {
  const [workload, setWorkload] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadWorkload() {
      try {
        setLoading(true);
        setError("");
        const response = await apiFetch(`${API_BASE_URL}/api/dashboard/team-workload`);
        if (!response.ok) {
          throw new Error(`Workload request failed (${response.status})`);
        }
        const data = await response.json();
        if (!cancelled) {
          setWorkload(data);
        }
      } catch (loadError) {
        console.error("Failed to load team workload:", loadError);
        if (!cancelled) {
          setError("Unable to load team workload.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadWorkload();
    return () => {
      cancelled = true;
    };
  }, [refreshKey]);

  return (
    <section className="workload-dashboard">
      <div className="dashboard-heading">
        <div>
          <h1>Team Workload</h1>
          <p>Active workload includes Draft, Doing, and Review tasks.</p>
        </div>
      </div>

      {error && <p className="form-error" role="alert">{error}</p>}
      {loading && <p className="dashboard-status">Loading team workload...</p>}
      {!loading && !error && workload.length === 0 && (
        <div className="empty-state"><p>No staff found.</p></div>
      )}

      <div className="workload-grid">
        {workload.map((staff) => {
          const activeTasks = staff.activeTaskCount ?? (staff.draftCount + staff.doingCount + staff.reviewCount);
          const barWidth = staff.totalWorkload > 0
            ? `${Math.min(100, staff.totalWorkload * 8)}%`
            : "0%";

          return (
            <article className="workload-card" key={staff.userId}>
              <div className="workload-card-header">
                <div>
                  <h2>{staff.name}</h2>
                  <p>{staff.departmentName}</p>
                </div>
                <div className="workload-total">
                  <strong>{staff.totalWorkload}</strong>
                  <span>workload</span>
                </div>
              </div>

              <div className="workload-bar" aria-label={`${staff.name} workload ${staff.totalWorkload}`}>
                <span style={{ width: barWidth }} />
              </div>

              <div className="workload-summary">
                <span>Active tasks <strong>{activeTasks}</strong></span>
                <span>Draft <strong>{staff.draftCount}</strong></span>
                <span>Doing <strong>{staff.doingCount}</strong></span>
                <span>Review <strong>{staff.reviewCount}</strong></span>
                <span>Done <strong>{staff.doneCount}</strong></span>
              </div>

              <button type="button" className="workload-view-button" onClick={() => onViewKanban(staff.userId)}>
                View Kanban
              </button>
            </article>
          );
        })}
      </div>
    </section>
  );
}

export default ManagerDashboard;
