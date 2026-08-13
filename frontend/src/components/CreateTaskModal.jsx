import { useState } from "react";
import { apiFetch } from "../api/apiFetch";

const API_BASE_URL = "http://localhost:8080";

const INITIAL_FORM = {
  title: "",
  description: "",
  priority: "MEDIUM",
  dueDate: "",
  assigneeId: "",
  workload: "3",
};

function CreateTaskModal({ isOpen, column, users, onClose, onCreated }) {
  const [formData, setFormData] = useState(INITIAL_FORM);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen || !column) {
    return null;
  }

  function handleChange(event) {
    const { name, value } = event.target;

    setFormData((currentForm) => ({
      ...currentForm,
      [name]: value,
    }));
    setError("");
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    const title = formData.title.trim();

    if (!title) {
      setError("Title is required.");
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await apiFetch(`${API_BASE_URL}/api/tasks`, {
        method: "POST",
        body: JSON.stringify({
          title,
          description: formData.description.trim() || null,
          priority: formData.priority,
          dueDate: formData.dueDate || null,
          columnId: Number(column.id),
          assigneeId: formData.assigneeId
            ? Number(formData.assigneeId)
            : null,
          workload: Number(formData.workload),
        }),
      });

      if (!response.ok) {
        let message = `Task creation failed (${response.status}).`;

        try {
          const responseBody = await response.json();
          message = responseBody.message || responseBody.error || message;
        } catch {
          // Keep the status-based message when the server does not return JSON.
        }

        throw new Error(message);
      }

      await onCreated();
      setFormData(INITIAL_FORM);
      onClose();
    } catch (submitError) {
      console.error("Failed to create task:", submitError);
      setError(submitError.message || "Unable to create task. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleBackdropClick(event) {
    if (event.target === event.currentTarget && !isSubmitting) {
      onClose();
    }
  }

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={handleBackdropClick}
    >
      <div
        className="modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-task-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="modal-header">
          <div>
            <h2 id="create-task-title">Create Task</h2>
            <p>Adding to {column.name}</p>
          </div>
          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            disabled={isSubmitting}
            aria-label="Close dialog"
          >
            &times;
          </button>
        </div>

        <form className="create-task-form" onSubmit={handleSubmit}>
          <label htmlFor="task-title">
            Title <span className="required-marker">*</span>
          </label>
          <input
            id="task-title"
            name="title"
            type="text"
            value={formData.title}
            onChange={handleChange}
            placeholder="Enter a task title"
            required
            autoFocus
          />

          <label htmlFor="task-description">Description</label>
          <textarea
            id="task-description"
            name="description"
            value={formData.description}
            onChange={handleChange}
            placeholder="Add more details (optional)"
            rows="4"
          />

          <div className="form-field-row">
            <div className="form-field">
              <label htmlFor="task-priority">Priority</label>
              <select
                id="task-priority"
                name="priority"
                value={formData.priority}
                onChange={handleChange}
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </select>
            </div>

            <div className="form-field">
              <label htmlFor="task-due-date">Due Date</label>
              <input
                id="task-due-date"
                name="dueDate"
                type="date"
                value={formData.dueDate}
                onChange={handleChange}
              />
            </div>
          </div>

          <label htmlFor="task-workload">Workload</label>
          <select
            id="task-workload"
            name="workload"
            value={formData.workload}
            onChange={handleChange}
          >
            <option value="1">1 — Very Small</option>
            <option value="2">2 — Small</option>
            <option value="3">3 — Medium</option>
            <option value="4">4 — Large</option>
            <option value="5">5 — Very Large</option>
          </select>

          <label htmlFor="task-assignee">Assignee</label>
          <select
            id="task-assignee"
            name="assigneeId"
            value={formData.assigneeId}
            onChange={handleChange}
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

          <div className="modal-actions">
            <button
              type="button"
              className="cancel-button"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="create-button"
              disabled={isSubmitting}
            >
              {isSubmitting ? "Creating..." : "Create Task"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default CreateTaskModal;
