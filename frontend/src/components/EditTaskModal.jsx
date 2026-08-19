import { useState } from "react";
import { apiFetch } from "../api/apiFetch";

const API_BASE_URL = "";

function EditTaskModal({ task, users, onClose, onTaskUpdated, onDelete, canDelete }) {
  const [title, setTitle] = useState(task?.title || "");
  const [description, setDescription] = useState(task?.description || "");
  const [priority, setPriority] = useState(task?.priority || "MEDIUM");
  const [dueDate, setDueDate] = useState(task?.dueDate || "");
  const [assigneeId, setAssigneeId] = useState(
    task?.assigneeId != null ? String(task.assigneeId) : ""
  );
  const [workload, setWorkload] = useState(String(task?.workload ?? 3));
  const [error, setError] = useState("");

  if (!task) {
    return null;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    try {
      const response = await apiFetch(`${API_BASE_URL}/api/tasks/${task.id}`, {
        method: "PUT",
        body: JSON.stringify({
          title,
          description,
          priority,
          dueDate: dueDate || null,
          assigneeId: assigneeId === "" ? null : Number(assigneeId),
          workload: Number(workload),
        }),
      });

      if (!response.ok) {
        throw new Error(response.status === 403
          ? "You do not have permission to perform this action."
          : "Unable to update task.");
      }

      onTaskUpdated();
      onClose();
    } catch (err) {
      console.error(err);
      setError("Unable to update task.");
    }
  }

  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div
        className="modal-card"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="modal-header">
          <div>
            <h2>Edit Task</h2>
            <p>Update the task details without moving it.</p>
          </div>

          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            aria-label="Close dialog"
          >
            &times;
          </button>
        </div>

        <form className="create-task-form" onSubmit={handleSubmit}>
          <label htmlFor="edit-task-title">
            Title <span className="required-marker">*</span>
          </label>
          <input
            id="edit-task-title"
            type="text"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            required
          />

          <label htmlFor="edit-task-description">Description</label>
          <textarea
            id="edit-task-description"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            rows="4"
          />

          <div className="form-field-row">
            <div className="form-field">
              <label htmlFor="edit-task-priority">Priority</label>
              <select
                id="edit-task-priority"
                value={priority}
                onChange={(event) => setPriority(event.target.value)}
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </select>
            </div>

            <div className="form-field">
              <label htmlFor="edit-task-due-date">Due Date</label>
              <input
                id="edit-task-due-date"
                type="date"
                value={dueDate}
                onChange={(event) => setDueDate(event.target.value)}
              />
            </div>
          </div>

          <label htmlFor="edit-task-workload">Workload</label>
          <select
            id="edit-task-workload"
            value={workload}
            onChange={(event) => setWorkload(event.target.value)}
          >
            <option value="1">1 — Very Small</option>
            <option value="2">2 — Small</option>
            <option value="3">3 — Medium</option>
            <option value="4">4 — Large</option>
            <option value="5">5 — Very Large</option>
          </select>

          <label htmlFor="edit-task-assignee">Assignee</label>
          <select
            id="edit-task-assignee"
            value={assigneeId}
            onChange={(event) => setAssigneeId(event.target.value)}
          >
            <option value="">Unassigned</option>
            {users.map((user) => (
              <option key={user.id} value={user.id}>
                {user.name || user.email}
              </option>
            ))}
          </select>

          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}

          <div className="modal-actions modal-actions-split">
            {canDelete && (
              <button
                type="button"
                className="delete-button"
                onClick={() => onDelete(task)}
              >
                Delete
              </button>
            )}

            <div className="modal-actions-right">
              <button
                type="button"
                className="cancel-button"
                onClick={onClose}
              >
                Cancel
              </button>
              <button type="submit" className="create-button">
                Save Changes
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}

export default EditTaskModal;
