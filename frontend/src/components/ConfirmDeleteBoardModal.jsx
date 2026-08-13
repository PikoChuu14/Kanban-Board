function ConfirmDeleteBoardModal({ board, deleting, error, onCancel, onConfirm }) {
  if (!board) {
    return null;
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div className="confirm-modal" onClick={(event) => event.stopPropagation()}>
        <h2>Delete Board?</h2>

        <p>
          Are you sure you want to delete <strong>"{board.name}"</strong>?
        </p>

        <p className="delete-warning">
          The board must be empty before it can be deleted.
        </p>

        {error && (
          <p className="delete-error" role="alert">
            {error}
          </p>
        )}

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

export default ConfirmDeleteBoardModal;
