package com.company.kanban.service;

import com.company.kanban.dto.CreateTaskRequest;
import com.company.kanban.dto.MoveTaskRequest;
import com.company.kanban.dto.TaskResponse;
import com.company.kanban.dto.UpdateTaskRequest;
import com.company.kanban.dto.UpdateTaskStatusRequest;
import com.company.kanban.dto.ReviewAction;
import com.company.kanban.dto.ReviewActionRequest;
import com.company.kanban.dto.ReassignTaskRequest;
import com.company.kanban.dto.ReviewQueueItem;
import com.company.kanban.entity.KanbanColumn;
import com.company.kanban.entity.Task;
import com.company.kanban.entity.TaskStatus;
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

    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(User currentUser) {

        return taskRepository
                .findByAssigneeIdOrderByStatusAscPositionAsc(
                        currentUser.getId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByUser(Long userId, User currentUser) {
        User staffUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        authorizationService.requireStaffViewerAccess(currentUser, staffUser);

        return taskRepository.findByAssigneeIdOrderByStatusAscPositionAsc(userId)
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
        task.setCreatedBy(currentUser);
        task.setWorkload(request.workload());
        task.setStatus(statusFromColumn(column));

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
        task.setWorkload(request.workload());

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
        authorizationService.requireTaskOwnerMove(currentUser, task);

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
        task.setStatus(statusFromColumn(targetColumn));
        if (task.getStatus() == TaskStatus.REVIEW) task.setSubmittedForReviewAt(java.time.LocalDateTime.now());
        else task.setSubmittedForReviewAt(null);

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
    public TaskResponse updateTaskStatus(
            Long taskId,
            UpdateTaskStatusRequest request,
            User currentUser) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"));
        authorizationService.requirePersonalStatusMove(
                currentUser,
                task,
                request.status()
        );

        KanbanColumn sourceColumn = task.getColumn();
        String targetColumnName = columnNameFromStatus(request.status());
        KanbanColumn targetColumn = kanbanColumnRepository
                .findByBoardIdAndName(
                        sourceColumn.getBoard().getId(),
                        targetColumnName
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Target status column is not configured for this board"
                ));

        List<Task> targetTasks = taskRepository
                .findByColumnIdAndIdNotOrderByPositionAsc(
                        targetColumn.getId(), taskId);
        int targetPosition = Math.max(
                1,
                Math.min(request.targetPosition(), targetTasks.size() + 1)
        );

        task.setColumn(targetColumn);
        task.setStatus(request.status());
        if (request.status() == TaskStatus.REVIEW) task.setSubmittedForReviewAt(java.time.LocalDateTime.now());
        if (request.status() != TaskStatus.REVIEW) task.setSubmittedForReviewAt(null);
        targetTasks.add(targetPosition - 1, task);
        normalizePositions(targetTasks);
        taskRepository.saveAll(targetTasks);

        if (!sourceColumn.getId().equals(targetColumn.getId())) {
            List<Task> sourceTasks = taskRepository
                    .findByColumnIdOrderByPositionAsc(sourceColumn.getId());
            normalizePositions(sourceTasks);
            taskRepository.saveAll(sourceTasks);
        }

        return toResponse(task);
    }

    @Transactional
    public TaskResponse reviewAction(
            Long taskId,
            ReviewActionRequest request,
            User currentUser) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"));
        authorizationService.requireReviewActionAccess(currentUser, task);

        if (task.getStatus() != TaskStatus.REVIEW) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Review actions are only available for tasks in REVIEW"
            );
        }

        TaskStatus targetStatus = request.action() == ReviewAction.APPROVE
                ? TaskStatus.DONE
                : TaskStatus.DOING;
        KanbanColumn sourceColumn = task.getColumn();
        KanbanColumn targetColumn = kanbanColumnRepository
                .findByBoardIdAndName(
                        sourceColumn.getBoard().getId(),
                        columnNameFromStatus(targetStatus)
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Target status column is not configured for this board"
                ));

        List<Task> targetTasks = taskRepository
                .findByColumnIdAndIdNotOrderByPositionAsc(
                        targetColumn.getId(), taskId);
        task.setColumn(targetColumn);
        task.setStatus(targetStatus);
        if (request.action() == ReviewAction.RETURN) task.setSubmittedForReviewAt(null);
        targetTasks.add(task);
        normalizePositions(targetTasks);
        taskRepository.saveAll(targetTasks);

        List<Task> sourceTasks = taskRepository
                .findByColumnIdOrderByPositionAsc(sourceColumn.getId());
        normalizePositions(sourceTasks);
        taskRepository.saveAll(sourceTasks);

        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<ReviewQueueItem> reviewQueue(User currentUser) {
        authorizationService.requireWorkloadDashboardAccess(currentUser);
        return taskRepository.findByStatusOrderBySubmittedForReviewAtAsc(TaskStatus.REVIEW).stream()
                .filter(task -> authorizationService.canAccessDepartment(currentUser, task.getColumn().getBoard().getDepartment().getId()))
                .map(task -> new ReviewQueueItem(task.getId(), task.getTitle(), task.getDescription(), task.getWorkload(), task.getPriority(),
                        task.getDueDate(), task.getAssignee() == null ? null : task.getAssignee().getId(),
                        task.getAssignee() == null ? null : task.getAssignee().getName(), task.getColumn().getBoard().getId(),
                        task.getColumn().getBoard().getName(), task.getColumn().getBoard().getDepartment().getId(),
                        task.getColumn().getBoard().getDepartment().getName(), task.getSubmittedForReviewAt(), task.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public TaskResponse reassignTask(Long taskId, ReassignTaskRequest request, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        User assignee = userRepository.findById(request.assigneeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee not found"));

        authorizationService.requireTaskReassignmentAccess(currentUser, task, assignee);
        task.setAssignee(assignee);
        return toResponse(taskRepository.save(task));
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
        User createdBy = task.getCreatedBy();
        KanbanColumn column = task.getColumn();

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStatus(),
                task.getWorkload(),
                task.getDueDate(),
                task.getPosition(),

                column.getBoard().getId(),
                column.getBoard().getName(),

                column.getId(),
                column.getName(),

                assignee != null ? assignee.getId() : null,
                assignee != null ? assignee.getName() : null,

                createdBy != null ? createdBy.getId() : null,
                createdBy != null ? createdBy.getName() : null,

                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private TaskStatus statusFromColumn(KanbanColumn column) {
        return switch (column.getName()) {
            case "To Do" -> TaskStatus.DRAFT;
            case "In Progress" -> TaskStatus.DOING;
            case "Review" -> TaskStatus.REVIEW;
            case "Done" -> TaskStatus.DONE;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported Kanban column"
            );
        };
    }

    private String columnNameFromStatus(TaskStatus status) {
        return switch (status) {
            case DRAFT -> "To Do";
            case DOING -> "In Progress";
            case REVIEW -> "Review";
            case DONE -> "Done";
        };
    }

    private void normalizePositions(List<Task> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).setPosition(i + 1);
        }
    }
}
