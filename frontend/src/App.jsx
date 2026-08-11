import { useEffect, useState } from "react";
import "./App.css";

function App() {
  const boardId = 1;

  const [columns, setColumns] = useState([]);
  const [tasksByColumn, setTasksByColumn] = useState({});

  useEffect(() => {
    fetch(`http://localhost:8080/api/columns/board/${boardId}`)
      .then((response) => response.json())
      .then(async (columnData) => {
        setColumns(columnData);

        const taskEntries = await Promise.all(
          columnData.map(async (column) => {
            const response = await fetch(
              `http://localhost:8080/api/tasks/column/${column.id}`
            );

            const tasks = await response.json();

            return [column.id, tasks];
          })
        );

        setTasksByColumn(
          Object.fromEntries(taskEntries)
        );
      })
      .catch((error) => {
        console.error("Failed to load board:", error);
      });
  }, []);

  return (
    <div className="app">
      <h1>PPC Workflow Board</h1>

      <div className="kanban-board">
        {columns.map((column) => (
          <div
            key={column.id}
            className="kanban-column"
          >
            <h2>{column.name}</h2>

            <div className="task-list">
              {(tasksByColumn[column.id] || []).map((task) => (
                <div
                  key={task.id}
                  className="task-card"
                >
                  <h3>{task.title}</h3>

                  <p>{task.description}</p>

                  <div className="task-meta">
                    <span>{task.priority}</span>

                    {task.assigneeName && (
                      <span>
                        {task.assigneeName}
                      </span>
                    )}
                  </div>
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