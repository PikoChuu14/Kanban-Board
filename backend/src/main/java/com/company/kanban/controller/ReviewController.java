package com.company.kanban.controller;

import com.company.kanban.dto.ReviewQueueItem;
import com.company.kanban.entity.User;
import com.company.kanban.service.TaskService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final TaskService taskService;
    public ReviewController(TaskService taskService) { this.taskService = taskService; }
    @GetMapping
    public List<ReviewQueueItem> queue(@AuthenticationPrincipal User currentUser) { return taskService.reviewQueue(currentUser); }
}
