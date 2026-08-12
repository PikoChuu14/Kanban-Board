import { useCallback, useEffect, useRef, useState } from "react";
import "./App.css";
import CreateTaskModal from "./components/CreateTaskModal";
import ConfirmDeleteModal from "./components/ConfirmDeleteModal";
import EditTaskModal from "./components/EditTaskModal";

const API_BASE_URL = "http://localhost:8080";
const DRAG_START_THRESHOLD = 6;

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
  const [users, setUsers] = useState([]);
  const [selectedColumn, setSelectedColumn] = useState(null);
  const [selectedTask, setSelectedTask] = useState(null);
  const [taskToDelete, setTaskToDelete] = useState(null);
  const [deletingTask, setDeletingTask] = useState(false);
  const [draggedTask, setDraggedTask] = useState(null);
  const [dropIndicator, setDropIndicator] = useState(null);
  const [dragPreview, setDragPreview] = useState(null);
  const draggedTaskRef = useRef(null);
  const dropIndicatorRef = useRef(null);
  const pointerDownRef = useRef(null);
  const tasksByColumnRef = useRef(tasksByColumn);
  const moveTaskRef = useRef(null);

  const loadBoard = useCallback(async () => {
    try {
      const columnsResponse = await fetch(
        `${API_BASE_URL}/api/columns/board/${boardId}`
      );

      if (!columnsResponse.ok) {
        throw new Error(`Columns request failed (${columnsResponse.status}).`);
      }

      const columnData = await columnsResponse.json();
      setColumns(columnData);

      const taskEntries = await Promise.all(
        columnData.map(async (column) => {
          const taskResponse = await fetch(
            `${API_BASE_URL}/api/tasks/column/${column.id}`
          );

          if (!taskResponse.ok) {
            throw new Error(
              `Tasks request failed for column ${column.id} (${taskResponse.status}).`
            );
          }

          const tasks = await taskResponse.json();

          return [column.id, tasks];
        })
      );

      setTasksByColumn(Object.fromEntries(taskEntries));
    } catch (error) {
      console.error("Failed to load Kanban board:", error);
    }
  }, [boardId]);

  useEffect(() => {
    async function initializeBoard() {
      await loadBoard();
    }

    initializeBoard();
  }, [loadBoard]);

  useEffect(() => {
    async function loadUsers() {
      try {
        const response = await fetch(`${API_BASE_URL}/api/users`);

        if (!response.ok) {
          throw new Error(`Users request failed (${response.status}).`);
        }

        setUsers(await response.json());
      } catch (error) {
        console.error("Failed to load users:", error);
      }
    }

    loadUsers();
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
    pointerDownRef.current = null;
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

    const card = event.currentTarget;
    const rect = card.getBoundingClientRect();
    pointerDownRef.current = {
      task,
      startX: event.clientX,
      startY: event.clientY,
      offsetX: event.clientX - rect.left,
      offsetY: event.clientY - rect.top,
      width: rect.width,
    };
  }

  function shouldStartDrag(pointerDown, event) {
    const distanceX = Math.abs(event.clientX - pointerDown.startX);
    const distanceY = Math.abs(event.clientY - pointerDown.startY);

    return Math.max(distanceX, distanceY) >= DRAG_START_THRESHOLD;
  }

  function startDrag(pointerDown, event) {
    draggedTaskRef.current = pointerDown.task;
    dropIndicatorRef.current = null;
    setDraggedTask(pointerDown.task);
    setDropIndicator(null);
    setDragPreview({
      task: pointerDown.task,
      x: event.clientX,
      y: event.clientY,
      offsetX: pointerDown.offsetX,
      offsetY: pointerDown.offsetY,
      width: pointerDown.width,
    });
  }

  useEffect(() => {
    function handlePointerMove(event) {
      const pointerDown = pointerDownRef.current;
      const task = draggedTaskRef.current;

      if (!pointerDown) {
        return;
      }

      if (!task) {
        if (!shouldStartDrag(pointerDown, event)) {
          return;
        }

        startDrag(pointerDown, event);
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
      const pointerDown = pointerDownRef.current;

      if (!task) {
        if (pointerDown) {
          clearDragState();
          setSelectedTask(pointerDown.task);
        }

        return;
      }

      clearDragState();

      if (!indicator) {
        return;
      }

      if (indicator.taskId === null) {
        const targetTasks = tasksByColumnRef.current[indicator.columnId] || [];

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

      let targetPosition =
        targetTask.position + (indicator.position === "after" ? 1 : 0);

      if (
        targetTask.columnId === task.columnId &&
        targetTask.position > task.position
      ) {
        targetPosition -= 1;
      }

      if (
        targetTask.columnId === task.columnId &&
        targetPosition === task.position
      ) {
        return;
      }

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

  const moveTask = useCallback(
    async (task, targetColumnId, targetPosition) => {
      try {
        const response = await fetch(
          `${API_BASE_URL}/api/tasks/${task.id}/move`,
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
    },
    [loadBoard]
  );

  useEffect(() => {
    moveTaskRef.current = moveTask;
  }, [moveTask]);

  function openCreateTaskModal(column) {
    setSelectedColumn(column);
  }

  function closeCreateTaskModal() {
    setSelectedColumn(null);
  }

  function requestDeleteTask(task) {
    setSelectedTask(null);
    setTaskToDelete(task);
  }

  async function confirmDeleteTask() {
    if (!taskToDelete) {
      return;
    }

    setDeletingTask(true);

    try {
      const response = await fetch(`${API_BASE_URL}/api/tasks/${taskToDelete.id}`, {
        method: "DELETE",
      });

      if (!response.ok) {
        throw new Error("Failed to delete task");
      }

      setTaskToDelete(null);

      await loadBoard();
    } catch (error) {
      console.error("Failed to delete task:", error);
    } finally {
      setDeletingTask(false);
    }
  }

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
            <div className="column-header">
              <h2>{column.name}</h2>
              <button
                type="button"
                className="add-task-button"
                onClick={() => openCreateTaskModal(column)}
              >
                + Add Task
              </button>
            </div>

            <div className="task-list">
              {(tasksByColumn[column.id] || []).map((task) => (
                <div key={task.id} className="task-wrapper">
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

      {selectedColumn && (
        <CreateTaskModal
          isOpen
          column={selectedColumn}
          users={users}
          onClose={closeCreateTaskModal}
          onCreated={loadBoard}
        />
      )}

      <EditTaskModal
        key={selectedTask?.id ?? "closed"}
        task={selectedTask}
        users={users}
        onClose={() => setSelectedTask(null)}
        onTaskUpdated={loadBoard}
        onDelete={requestDeleteTask}
      />

      <ConfirmDeleteModal
        task={taskToDelete}
        deleting={deletingTask}
        onCancel={() => setTaskToDelete(null)}
        onConfirm={confirmDeleteTask}
      />
    </div>
  );
}

export default App;
