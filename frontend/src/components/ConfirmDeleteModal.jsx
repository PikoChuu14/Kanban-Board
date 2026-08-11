function ConfirmDeleteModal({ task, onCancel, onConfirm, deleting }) {
  if (!task) {
    return null;
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div className="confirm-modal" onClick={(event) => event.stopPropagation()}>
        <h2>Delete Task?</h2>

        <p>
          Are you sure you want to delete <strong>{task.title}</strong>?
        </p>

        <p className="delete-warning">This action cannot be undone.</p>

        <div className="modal-actions">
          <button type="button" onClick={onCancel} disabled={deleting}>
            Cancel
          </button>

          <button
            type="button"
            className="delete-button"
            onClick={onConfirm}
            disabled={deleting}
          >
            {deleting ? "Deleting..." : "Delete"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default ConfirmDeleteModal;
