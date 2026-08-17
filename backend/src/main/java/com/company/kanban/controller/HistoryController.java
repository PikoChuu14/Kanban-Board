package com.company.kanban.controller;

import com.company.kanban.dto.SnapshotDateResponse;
import com.company.kanban.dto.TaskSnapshotResponse;
import com.company.kanban.entity.SnapshotType;
import com.company.kanban.entity.User;
import com.company.kanban.service.TaskSnapshotService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {
    private final TaskSnapshotService service;
    public HistoryController(TaskSnapshotService service) { this.service = service; }

    @GetMapping("/dates")
    public List<SnapshotDateResponse> dates() { return service.getDates(); }

    @GetMapping("/{date}")
    public List<TaskSnapshotResponse> history(@PathVariable LocalDate date,
                                              @RequestParam(required = false) SnapshotType type,
                                              @AuthenticationPrincipal User currentUser) {
        return service.getHistory(date, type, currentUser);
    }

    @GetMapping("/{date}/users/{userId}")
    public List<TaskSnapshotResponse> userHistory(@PathVariable LocalDate date, @PathVariable Long userId,
                                                  @RequestParam(required = false) SnapshotType type,
                                                  @AuthenticationPrincipal User currentUser) {
        return service.getUserHistory(date, type, userId, currentUser);
    }
}
