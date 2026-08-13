package com.company.kanban.controller;

import com.company.kanban.dto.CreateTaskRequest;
import com.company.kanban.dto.MoveTaskRequest;
import com.company.kanban.dto.TaskResponse;
import com.company.kanban.dto.UpdateTaskRequest;
import com.company.kanban.service.TaskService;
import com.company.kanban.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/column/{columnId}")
    public List<TaskResponse> getTasksByColumn(
            @PathVariable Long columnId,
            @AuthenticationPrincipal User currentUser) {

        return taskService.getTasksByColumn(columnId, currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @Valid
            @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal User currentUser) {

        return taskService.createTask(request, currentUser);
    }

    @PutMapping("/{taskId}/move")
    public TaskResponse moveTask(
            @PathVariable Long taskId,
            @Valid @RequestBody MoveTaskRequest request,
            @AuthenticationPrincipal User currentUser) {

        return taskService.moveTask(taskId, request, currentUser);
    }

    @PutMapping("/{taskId}")
    public TaskResponse updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal User currentUser) {

        return taskService.updateTask(taskId, request, currentUser);
    }

    @DeleteMapping("/{taskId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser) {

        taskService.deleteTask(taskId, currentUser);
    }
}
