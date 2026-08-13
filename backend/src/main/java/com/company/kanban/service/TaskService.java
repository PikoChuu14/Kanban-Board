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
    private final AuthorizationService authorizationService;

    public TaskService(
            TaskRepository taskRepository,
            KanbanColumnRepository kanbanColumnRepository,
            UserRepository userRepository,
            AuthorizationService authorizationService) {

        this.taskRepository = taskRepository;
        this.kanbanColumnRepository = kanbanColumnRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByColumn(Long columnId, User currentUser) {

        KanbanColumn column = kanbanColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Column not found"));
        authorizationService.requireColumnAccess(currentUser, column);

        return taskRepository
                .findByColumnIdOrderByPositionAsc(columnId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, User currentUser) {

        KanbanColumn column =
                kanbanColumnRepository.findById(request.columnId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Column not found"
                                )
                        );
        authorizationService.requireColumnAccess(currentUser, column);

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
            authorizationService.requireAssignableUser(currentUser, assignee);
            authorizationService.requireAssigneeMatchesTaskDepartment(
                    assignee,
                    column.getBoard().getDepartment()
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
            UpdateTaskRequest request,
            User currentUser) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Task not found"
                        )
                );
        authorizationService.requireTaskAccess(currentUser, task);

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
            authorizationService.requireAssignableUser(currentUser, assignee);
            authorizationService.requireAssigneeMatchesTaskDepartment(
                    assignee,
                    task.getColumn().getBoard().getDepartment()
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
            MoveTaskRequest request,
            User currentUser) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Task not found"
                        )
                );
        authorizationService.requireTaskAccess(currentUser, task);

        KanbanColumn sourceColumn = task.getColumn();

        KanbanColumn targetColumn =
                kanbanColumnRepository.findById(request.targetColumnId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Target column not found"
                                )
                        );
        authorizationService.requireColumnAccess(currentUser, targetColumn);

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

    @Transactional
    public void deleteTask(Long taskId, User currentUser) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Task not found"
                        )
                );
        authorizationService.requireTaskAccess(currentUser, task);

        Long columnId = task.getColumn().getId();

        taskRepository.delete(task);

        taskRepository.flush();

        List<Task> remainingTasks =
                taskRepository.findByColumnIdOrderByPositionAsc(columnId);

        for (int i = 0; i < remainingTasks.size(); i++) {
            remainingTasks.get(i).setPosition(i + 1);
        }

        taskRepository.saveAll(remainingTasks);
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
