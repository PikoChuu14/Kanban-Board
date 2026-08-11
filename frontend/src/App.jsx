import { useEffect, useState } from "react";
import "./App.css";

function App() {
  const boardId = 2;

  const [columns, setColumns] = useState([]);
  const [tasksByColumn, setTasksByColumn] = useState({});
  const [draggedTask, setDraggedTask] = useState(null);

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
    loadBoard();
  }, []);

  function handleDragStart(task) {
    setDraggedTask(task);
  }

  function handleDragOver(event) {
    event.preventDefault();
  }

  async function handleDrop(column) {
    if (!draggedTask) {
      return;
    }

    const targetTasks = tasksByColumn[column.id] || [];

    const targetPosition = targetTasks.length + 1;

    try {
      const response = await fetch(
        `http://localhost:8080/api/tasks/${draggedTask.id}/move`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            targetColumnId: column.id,
            targetPosition: targetPosition,
          }),
        }
      );

      if (!response.ok) {
        throw new Error("Failed to move task");
      }

      setDraggedTask(null);

      await loadBoard();
    } catch (error) {
      console.error("Failed to move task:", error);
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
            onDragOver={handleDragOver}
            onDrop={() => handleDrop(column)}
          >
            <h2>{column.name}</h2>

            <div className="task-list">
              {(tasksByColumn[column.id] || []).map((task) => (
                <div
                  className="task-card"
                  key={task.id}
                  draggable
                  onDragStart={() => handleDragStart(task)}
                >
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
    </div>
  );
}

export default App;