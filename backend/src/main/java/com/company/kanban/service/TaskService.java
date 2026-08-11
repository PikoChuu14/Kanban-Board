package com.company.kanban.service;

import com.company.kanban.dto.CreateTaskRequest;
import com.company.kanban.dto.MoveTaskRequest;
import com.company.kanban.dto.TaskResponse;
import com.company.kanban.dto.UpdateTaskRequest;
import com.company.kanban.entity.KanbanColumn;
import com.company.kanban.entity.Task;
import com.company.kanban.entity.User;
import com.company.kanban.repository.KanbanColumnRepository;
import com.company.kanban.repository.TaskRepository;
import com.company.kanban.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final KanbanColumnRepository kanbanColumnRepository;
    private final UserRepository userRepository;

    public TaskService(
            TaskRepository taskRepository,
            KanbanColumnRepository kanbanColumnRepository,
            UserRepository userRepository) {

        this.taskRepository = taskRepository;
        this.kanbanColumnRepository = kanbanColumnRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByColumn(Long columnId) {

        return taskRepository
                .findByColumnIdOrderByPositionAsc(columnId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {

        KanbanColumn column =
                kanbanColumnRepository.findById(request.columnId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Column not found"
                                )
                        );

        User assignee = null;

        if (request.assigneeId() != null) {
            assignee = userRepository
                    .findById(request.assigneeId())
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Assignee not found"
                            )
                    );
        }

        int position =
                taskRepository.countByColumnId(column.getId()) + 1;

        Task task = new Task(
                request.title(),
                request.description(),
                request.priority(),
                request.dueDate(),
                position,
                column,
                assignee
        );

        Task savedTask = taskRepository.save(task);

        return toResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTask(
            Long taskId,
            UpdateTaskRequest request) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Task not found"
                        )
                );

        User assignee = null;

        if (request.assigneeId() != null) {
            assignee = userRepository
                    .findById(request.assigneeId())
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Assignee not found"
                            )
                    );
        }

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setAssignee(assignee);

        Task savedTask = taskRepository.save(task);

        return toResponse(savedTask);
    }

    @Transactional
    public TaskResponse moveTask(
            Long taskId,
            MoveTaskRequest request) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Task not found"
                        )
                );

        KanbanColumn sourceColumn = task.getColumn();

        KanbanColumn targetColumn =
                kanbanColumnRepository.findById(request.targetColumnId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Target column not found"
                                )
                        );

        if (!sourceColumn.getBoard().getId()
                .equals(targetColumn.getBoard().getId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot move task to another board"
            );
        }

        List<Task> targetTasks =
                taskRepository
                        .findByColumnIdAndIdNotOrderByPositionAsc(
                                targetColumn.getId(),
                                taskId
                        );

        int requestedPosition = request.targetPosition();

        int targetPosition = Math.max(
                1,
                Math.min(
                        requestedPosition,
                        targetTasks.size() + 1
                )
        );

        task.setColumn(targetColumn);

        targetTasks.add(
                targetPosition - 1,
                task
        );

        for (int i = 0; i < targetTasks.size(); i++) {
            targetTasks.get(i).setPosition(i + 1);
        }

        taskRepository.saveAll(targetTasks);

        if (!sourceColumn.getId()
                .equals(targetColumn.getId())) {

            List<Task> sourceTasks =
                    taskRepository
                            .findByColumnIdOrderByPositionAsc(
                                    sourceColumn.getId()
                            );

            for (int i = 0; i < sourceTasks.size(); i++) {
                sourceTasks.get(i).setPosition(i + 1);
            }

            taskRepository.saveAll(sourceTasks);
        }

        return toResponse(task);
    }

    private TaskResponse toResponse(Task task) {

        User assignee = task.getAssignee();

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getDueDate(),
                task.getPosition(),

                task.getColumn().getId(),
                task.getColumn().getName(),

                assignee != null ? assignee.getId() : null,
                assignee != null ? assignee.getName() : null,

                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}