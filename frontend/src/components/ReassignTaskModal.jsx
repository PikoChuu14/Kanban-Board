import { useEffect, useState } from "react";
import { apiFetch } from "../api/apiFetch";

const API_BASE_URL = "http://localhost:8080";

function ReassignTaskModal({ task, users, onClose, onReassigned }) {
  const [assigneeId, setAssigneeId] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    setAssigneeId("");
    setError("");
  }, [task]);

  if (!task) return null;

  async function handleSubmit(event) {
    event.preventDefault();
    if (!assigneeId) {
      setError("Choose a new assignee.");
      return;
    }

    setSaving(true);
    setError("");
    try {
      const response = await apiFetch(`${API_BASE_URL}/api/tasks/${task.id}/assignee`, {
        method: "PUT",
        body: JSON.stringify({ assigneeId: Number(assigneeId) }),
      });
      if (!response.ok) {
        const payload = await response.json().catch(() => null);
        throw new Error(payload?.message || "Unable to reassign this task.");
      }
      await onReassigned(await response.json());
      onClose();
    } catch (reassignError) {
      setError(reassignError.message || "Unable to reassign this task.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal-card" onMouseDown={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h2>Reassign Task</h2>
            <p>{task.title}</p>
          </div>
          <button type="button" className="modal-close-button" onClick={onClose} aria-label="Close dialog">&times;</button>
        </div>
        <form className="create-task-form" onSubmit={handleSubmit}>
          <label>Current assignee</label>
          <div className="current-assignee">{task.assigneeName || "Unassigned"}</div>
          <label htmlFor="reassign-assignee">New assignee</label>
          <select id="reassign-assignee" value={assigneeId} onChange={(event) => setAssigneeId(event.target.value)} disabled={saving}>
            <option value="">Select staff member</option>
            {users.filter((candidate) => candidate.role === "STAFF" && candidate.id !== task.assigneeId).map((candidate) => (
              <option key={candidate.id} value={candidate.id}>{candidate.name || candidate.email}</option>
            ))}
          </select>
          {error && <p className="form-error" role="alert">{error}</p>}
          <div className="modal-actions">
            <button type="button" className="cancel-button" onClick={onClose} disabled={saving}>Cancel</button>
            <button type="submit" className="create-button" disabled={saving}>{saving ? "Reassigning..." : "Reassign"}</button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default ReassignTaskModal;
