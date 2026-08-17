import { useState } from "react";
import { apiFetch } from "../api/apiFetch";

const API_BASE_URL = "http://localhost:8080";

function EditBoardModal({ board, onClose, onUpdated }) {
  const [formData, setFormData] = useState({
    name: board?.name ?? "",
    description: board?.description ?? "",
  });
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!board) {
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

    if (!name) {
      setError("Board name is required.");
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await apiFetch(`${API_BASE_URL}/api/boards/${board.id}`, {
        method: "PUT",
        body: JSON.stringify({
          name,
          description: formData.description.trim() || null,
        }),
      });

      if (!response.ok) {
        let message = `Board update failed (${response.status}).`;

        try {
          const responseBody = await response.json();
          message = responseBody.message || responseBody.error || message;
        } catch {
          // Keep the status-based message when the server does not return JSON.
        }

        throw new Error(message);
      }

      await onUpdated(await response.json());
      onClose();
    } catch (submitError) {
      console.error("Failed to update board:", submitError);
      setError(submitError.message || "Unable to update board. Please try again.");
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
        aria-labelledby="edit-board-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="modal-header">
          <div>
            <h2 id="edit-board-title">Edit Board</h2>
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
          <label htmlFor="edit-board-name">
            Board Name <span className="required-marker">*</span>
          </label>
          <input
            id="edit-board-name"
            name="name"
            type="text"
            value={formData.name}
            onChange={handleChange}
            required
            autoFocus
          />

          <label htmlFor="edit-board-description">Description</label>
          <textarea
            id="edit-board-description"
            name="description"
            value={formData.description}
            onChange={handleChange}
            rows="4"
          />

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
              {isSubmitting ? "Saving..." : "Save Changes"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default EditBoardModal;
