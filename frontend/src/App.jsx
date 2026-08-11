import { useCallback, useEffect, useState } from "react";
import "./App.css";
import CreateTaskModal from "./components/CreateTaskModal";

const API_BASE_URL = "http://localhost:8080";

function App() {
  const boardId = 2;

  const [columns, setColumns] = useState([]);
  const [tasksByColumn, setTasksByColumn] = useState({});
  const [users, setUsers] = useState([]);
  const [selectedColumn, setSelectedColumn] = useState(null);

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

  function openCreateTaskModal(column) {
    setSelectedColumn(column);
  }

  function closeCreateTaskModal() {
    setSelectedColumn(null);
  }

  return (
    <div className="app">
      <h1>PPC Workflow Board</h1>

      <div className="kanban-board">
        {columns.map((column) => (
          <div className="kanban-column" key={column.id}>
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
                <div className="task-card" key={task.id}>
                  <h3>{task.title}</h3>

                  <p>{task.description}</p>

                  <div className="task-meta">
                    <span>{task.priority}</span>

                    {task.assigneeName && (
                      <span>{task.assigneeName}</span>
                    )}
                  </div>

                  {task.dueDate && (
                    <small>
                      Due: {task.dueDate}
                    </small>
                  )}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      {selectedColumn && (
        <CreateTaskModal
          isOpen
          column={selectedColumn}
          users={users}
          onClose={closeCreateTaskModal}
          onCreated={loadBoard}
        />
      )}
    </div>
  );
}

export default App;
