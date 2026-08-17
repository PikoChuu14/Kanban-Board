package com.company.kanban.service;

import com.company.kanban.entity.SnapshotType;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.time.*;

@Service
public class SnapshotRecoveryService {
    private final TaskSnapshotService snapshotService;

    public SnapshotRecoveryService(TaskSnapshotService snapshotService) { this.snapshotService = snapshotService; }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() { recoverMissingForToday(); }

    public void recoverMissingForToday() {
        ZonedDateTime now = ZonedDateTime.now(TaskSnapshotService.COMPANY_ZONE);
        LocalDate date = now.toLocalDate();
        if (!now.toLocalTime().isBefore(LocalTime.of(8, 0))) {
            snapshotService.createSnapshot(date, SnapshotType.START_OF_DAY, true);
        }
        if (!now.toLocalTime().isBefore(LocalTime.of(17, 0))) {
            snapshotService.createSnapshot(date, SnapshotType.END_OF_DAY, true);
        }
    }
}
