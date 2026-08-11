import { useEffect, useRef, useState } from "react";
import "./App.css";

function TaskCardContent({ task }) {
  return (
    <>
      <h3>{task.title}</h3>

      <p>{task.description}</p>

      <div className="task-meta">
        <span>{task.priority}</span>

        {task.assigneeName && <span>{task.assigneeName}</span>}
      </div>

      {task.dueDate && <small>Due: {task.dueDate}</small>}
    </>
  );
}

function App() {
  const boardId = 2;

  const [columns, setColumns] = useState([]);
  const [tasksByColumn, setTasksByColumn] = useState({});
  const [draggedTask, setDraggedTask] = useState(null);
  const [dropIndicator, setDropIndicator] = useState(null);
  const [dragPreview, setDragPreview] = useState(null);
  const draggedTaskRef = useRef(null);
  const dropIndicatorRef = useRef(null);
  const tasksByColumnRef = useRef(tasksByColumn);
  const moveTaskRef = useRef(null);

  async function loadBoard() {
    try {
      const columnsResponse = await fetch(
        `http://localhost:8080/api/columns/board/${boardId}`
      );

      const columnData = await columnsResponse.json();

      setColumns(columnData);

      const taskEntries = await Promise.all(
        columnData.map(async (column) => {
          const taskResponse = await fetch(
            `http://localhost:8080/api/tasks/column/${column.id}`
          );

          const tasks = await taskResponse.json();

          return [column.id, tasks];
        })
      );

      setTasksByColumn(Object.fromEntries(taskEntries));
    } catch (error) {
      console.error("Failed to load board:", error);
    }
  }

  useEffect(() => {
    // The initial fetch populates the board from the backend.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void loadBoard();
  }, []);

  useEffect(() => {
    tasksByColumnRef.current = tasksByColumn;
  }, [tasksByColumn]);

  function updateDropIndicator(indicator) {
    const previous = dropIndicatorRef.current;
    const isSameIndicator =
      previous?.columnId === indicator?.columnId &&
      previous?.taskId === indicator?.taskId &&
      previous?.position === indicator?.position;

    if (isSameIndicator) {
      return;
    }

    dropIndicatorRef.current = indicator;
    setDropIndicator(indicator);
  }

  function clearDragState() {
    draggedTaskRef.current = null;
    dropIndicatorRef.current = null;
    setDraggedTask(null);
    setDropIndicator(null);
    setDragPreview(null);
  }

  function getDropIndicator(clientX, clientY, task) {
    const element = document.elementFromPoint(clientX, clientY);
    const taskElement = element?.closest("[data-task-id]");

    if (taskElement) {
      const targetTaskId = Number(taskElement.dataset.taskId);
      const targetColumnId = Number(taskElement.dataset.columnId);

      if (targetTaskId === task.id) {
        return null;
      }

      const rect = taskElement.getBoundingClientRect();

      return {
        columnId: targetColumnId,
        taskId: targetTaskId,
        position: clientY < rect.top + rect.height / 2 ? "before" : "after",
      };
    }

    const columnElement = element?.closest("[data-column-id]");

    if (!columnElement) {
      return null;
    }

    return {
      columnId: Number(columnElement.dataset.columnId),
      taskId: null,
      position: "after",
    };
  }

  function handlePointerDown(event, task) {
    if (event.button !== 0) {
      return;
    }

    event.preventDefault();

    const card = event.currentTarget;
    const rect = card.getBoundingClientRect();
    const preview = {
      task,
      x: event.clientX,
      y: event.clientY,
      offsetX: event.clientX - rect.left,
      offsetY: event.clientY - rect.top,
      width: rect.width,
    };

    draggedTaskRef.current = task;
    dropIndicatorRef.current = null;
    setDraggedTask(task);
    setDropIndicator(null);
    setDragPreview(preview);
  }

  useEffect(() => {
    function handlePointerMove(event) {
      const task = draggedTaskRef.current;

      if (!task) {
        return;
      }

      setDragPreview((currentPreview) =>
        currentPreview
          ? {
              ...currentPreview,
              x: event.clientX,
              y: event.clientY,
            }
          : currentPreview
      );

      updateDropIndicator(getDropIndicator(event.clientX, event.clientY, task));
    }

    async function handlePointerUp() {
      const task = draggedTaskRef.current;
      const indicator = dropIndicatorRef.current;

      if (!task) {
        return;
      }

      clearDragState();

      if (!indicator) {
        return;
      }

      if (indicator.taskId === null) {
        const targetTasks =
          tasksByColumnRef.current[indicator.columnId] || [];

        await moveTaskRef.current(
          task,
          indicator.columnId,
          targetTasks.length + 1
        );
        return;
      }

      const targetTask = (
        tasksByColumnRef.current[indicator.columnId] || []
      ).find((candidate) => candidate.id === indicator.taskId);

      if (!targetTask) {
        return;
      }

      const targetPosition =
        targetTask.position + (indicator.position === "after" ? 1 : 0);

      await moveTaskRef.current(task, indicator.columnId, targetPosition);
    }

    function handlePointerCancel() {
      if (draggedTaskRef.current) {
        clearDragState();
      }
    }

    window.addEventListener("pointermove", handlePointerMove);
    window.addEventListener("pointerup", handlePointerUp);
    window.addEventListener("pointercancel", handlePointerCancel);

    return () => {
      window.removeEventListener("pointermove", handlePointerMove);
      window.removeEventListener("pointerup", handlePointerUp);
      window.removeEventListener("pointercancel", handlePointerCancel);
    };
  }, []);

  async function moveTask(task, targetColumnId, targetPosition) {
    try {
      const response = await fetch(
        `http://localhost:8080/api/tasks/${task.id}/move`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            targetColumnId,
            targetPosition,
          }),
        }
      );

      if (!response.ok) {
        throw new Error("Failed to move task");
      }

      setDraggedTask(null);
      setDropIndicator(null);
      await loadBoard();
    } catch (error) {
      console.error("Failed to move task:", error);
    }
  }

  useEffect(() => {
    moveTaskRef.current = moveTask;
  });

  return (
    <div className="app">
      <h1>PPC Workflow Board</h1>

      <div className="kanban-board">
        {columns.map((column) => (
          <div
            className="kanban-column"
            key={column.id}
            data-column-id={column.id}
          >
            <h2>{column.name}</h2>

            <div className="task-list">
              {(tasksByColumn[column.id] || []).map((task) => (
                <div
                  key={task.id}
                  className="task-wrapper"
                >
                  {dropIndicator?.columnId === column.id &&
                    dropIndicator?.taskId === task.id &&
                    dropIndicator?.position === "before" && (
                      <div className="drop-indicator" />
                    )}

                  <div
                    className={`task-card ${
                      draggedTask?.id === task.id ? "task-card--dragging" : ""
                    }`}
                    data-column-id={column.id}
                    data-task-id={task.id}
                    onPointerDown={(event) => handlePointerDown(event, task)}
                  >
                    <TaskCardContent task={task} />
                  </div>

                  {dropIndicator?.columnId === column.id &&
                    dropIndicator?.taskId === task.id &&
                    dropIndicator?.position === "after" && (
                      <div className="drop-indicator" />
                    )}
                </div>
              ))}

              {dropIndicator?.columnId === column.id &&
                dropIndicator?.taskId === null && (
                  <div className="drop-indicator" />
                )}
            </div>
          </div>
        ))}
      </div>

      {dragPreview && (
        <div
          className="drag-preview"
          style={{
            left: `${dragPreview.x - dragPreview.offsetX}px`,
            top: `${dragPreview.y - dragPreview.offsetY}px`,
            width: `${dragPreview.width}px`,
          }}
        >
          <div className="task-card drag-preview-card">
            <TaskCardContent task={dragPreview.task} />
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
