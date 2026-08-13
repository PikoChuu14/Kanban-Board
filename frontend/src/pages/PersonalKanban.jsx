import { useEffect, useState } from "react";
import { apiFetch } from "../api/apiFetch";

const API_BASE_URL = "http://localhost:8080";

const STATUSES = [
  { value: "DRAFT", label: "Draft" },
  { value: "DOING", label: "Doing" },
  { value: "REVIEW", label: "Review" },
  { value: "DONE", label: "Done" },
];

function PersonalKanban() {
  const [tasks, setTasks] = useState([]);

  useEffect(() => {
    async function loadMyTasks() {
      try {
        const response = await apiFetch(`${API_BASE_URL}/api/tasks/my`);

        if (!response.ok) {
          throw new Error("Failed to load personal tasks");
        }

        const data = await response.json();

        setTasks(data);
      } catch (error) {
        console.error("Failed to load personal Kanban:", error);
      }
    }

    loadMyTasks();
  }, []);

  return (
    <>
      <h1>My Work</h1>

      <div className="kanban-board personal-kanban">
        {STATUSES.map((status) => {
          const statusTasks = tasks.filter(
            (task) => task.status === status.value
          );

          return (
            <div className="kanban-column" key={status.value}>
              <div className="column-header">
                <h2>{status.label}</h2>
              </div>

              <div className="task-list">
                {statusTasks.map((task) => (
                  <div className="task-card" key={task.id}>
                    <h3>{task.title}</h3>

                    {task.boardName && (
                      <small className="task-board-name">{task.boardName}</small>
                    )}

                    <p>{task.description}</p>

                    <div className="task-meta">
                      <span>{task.priority} · Workload {task.workload ?? "—"}</span>
                    </div>

                    {task.dueDate && <small>Due: {task.dueDate}</small>}

                    {task.createdByName && (
                      <small>Created by {task.createdByName}</small>
                    )}
                  </div>
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </>
  );
}

export default PersonalKanban;
