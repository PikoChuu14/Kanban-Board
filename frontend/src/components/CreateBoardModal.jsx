import { useState } from "react";
import { apiFetch } from "../api/apiFetch";

const API_BASE_URL = "";
const INITIAL_FORM = {
  name: "",
  description: "",
  departmentId: "",
};

function CreateBoardModal({ isOpen, user, departments, onClose, onCreated }) {
  const [formData, setFormData] = useState(INITIAL_FORM);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const isAdmin = user?.role === "ADMIN";

  if (!isOpen || !user) {
    return null;
  }

  function handleChange(event) {
    const { name, value } = event.target;
    setFormData((currentForm) => ({ ...currentForm, [name]: value }));
    setError("");
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    const name = formData.name.trim();
    const departmentId = isAdmin
      ? Number(formData.departmentId)
      : user.departmentId;

    if (!name) {
      setError("Board name is required.");
      return;
    }

    if (!departmentId) {
      setError("Select a department.");
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await apiFetch(`${API_BASE_URL}/api/boards`, {
        method: "POST",
        body: JSON.stringify({
          name,
          description: formData.description.trim() || null,
          departmentId,
        }),
      });

      if (!response.ok) {
        if (response.status === 400 || response.status === 409) {
          throw new Error(
            "A board with this name already exists in this department."
          );
        }

        let message = `Board creation failed (${response.status}).`;

        try {
          const responseBody = await response.json();
          message = responseBody.message || responseBody.error || message;
        } catch {
          // Keep the status-based message when the server does not return JSON.
        }

        throw new Error(message);
      }

      const createdBoard = await response.json();
      await onCreated(createdBoard);
      setFormData(INITIAL_FORM);
      onClose();
    } catch (submitError) {
      console.error("Failed to create board:", submitError);
      setError(submitError.message || "Unable to create board. Please try again.");
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
        aria-labelledby="create-board-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="modal-header">
          <div>
            <h2 id="create-board-title">Create Board</h2>
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
          <label htmlFor="board-name">
            Board Name <span className="required-marker">*</span>
          </label>
          <input
            id="board-name"
            name="name"
            type="text"
            value={formData.name}
            onChange={handleChange}
            placeholder="Enter a board name"
            required
            autoFocus
          />

          <label htmlFor="board-description">Description</label>
          <textarea
            id="board-description"
            name="description"
            value={formData.description}
            onChange={handleChange}
            placeholder="Add a description (optional)"
            rows="4"
          />

          {isAdmin && (
            <>
              <label htmlFor="board-department">
                Department <span className="required-marker">*</span>
              </label>
              <select
                id="board-department"
                name="departmentId"
                value={formData.departmentId}
                onChange={handleChange}
                required
              >
                <option value="">Select a department</option>
                {departments.map((department) => (
                  <option key={department.id} value={department.id}>
                    {department.name}
                  </option>
                ))}
              </select>
            </>
          )}

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
            <button type="submit" className="create-button" disabled={isSubmitting}>
              {isSubmitting ? "Creating..." : "Create Board"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default CreateBoardModal;
