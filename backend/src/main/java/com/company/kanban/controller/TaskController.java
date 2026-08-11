package com.company.kanban.controller;

import com.company.kanban.dto.CreateTaskRequest;
import com.company.kanban.dto.MoveTaskRequest;
import com.company.kanban.dto.TaskResponse;
import com.company.kanban.dto.UpdateTaskRequest;
import com.company.kanban.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
            @PathVariable Long columnId) {

        return taskService.getTasksByColumn(columnId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @Valid
            @RequestBody CreateTaskRequest request) {

        return taskService.createTask(request);
    }

    @PutMapping("/{taskId}/move")
    public TaskResponse moveTask(
            @PathVariable Long taskId,
            @Valid @RequestBody MoveTaskRequest request) {

        return taskService.moveTask(taskId, request);
    }

    @PutMapping("/{taskId}")
    public TaskResponse updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {

        return taskService.updateTask(taskId, request);
    }
}