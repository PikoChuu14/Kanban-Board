package com.company.kanban.controller;

import com.company.kanban.dto.SnapshotBatchResponse;
import com.company.kanban.entity.SnapshotType;
import com.company.kanban.service.TaskSnapshotService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@Profile("dev")
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/dev/snapshots")
public class DevSnapshotController {
    private final TaskSnapshotService service;
    public DevSnapshotController(TaskSnapshotService service) { this.service = service; }

    @PostMapping("/start")
    public SnapshotBatchResponse start() { return service.createSnapshot(LocalDate.now(TaskSnapshotService.COMPANY_ZONE), SnapshotType.START_OF_DAY, false); }

    @PostMapping("/end")
    public SnapshotBatchResponse end() { return service.createSnapshot(LocalDate.now(TaskSnapshotService.COMPANY_ZONE), SnapshotType.END_OF_DAY, false); }
}
