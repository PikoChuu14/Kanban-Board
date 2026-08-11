import { useEffect, useState } from "react";
import "./App.css";

function App() {
  const boardId = 2;

  const [columns, setColumns] = useState([]);
  const [tasksByColumn, setTasksByColumn] = useState({});

  useEffect(() => {
    async function loadBoard() {
      try {
        // 1. Load columns
        const columnsResponse = await fetch(
          `http://localhost:8080/api/columns/board/${boardId}`
        );

        const columnData = await columnsResponse.json();

        setColumns(columnData);

        // 2. Load tasks for every column
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
        console.error("Failed to load Kanban board:", error);
      }
    }

    loadBoard();
  }, []);

  return (
    <div className="app">
      <h1>PPC Workflow Board</h1>

      <div className="kanban-board">
        {columns.map((column) => (
          <div className="kanban-column" key={column.id}>
            <h2>{column.name}</h2>

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
    </div>
  );
}

export default App;