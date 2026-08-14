package com.company.kanban.service;

import com.company.kanban.entity.SnapshotType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SnapshotScheduler {
    private final TaskSnapshotService snapshotService;
    private final SnapshotRecoveryService recoveryService;

    public SnapshotScheduler(TaskSnapshotService snapshotService, SnapshotRecoveryService recoveryService) {
        this.snapshotService = snapshotService;
        this.recoveryService = recoveryService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kuala_Lumpur")
    public void captureStartOfDay() { snapshotService.createSnapshot(LocalDate.now(TaskSnapshotService.COMPANY_ZONE), SnapshotType.START_OF_DAY, false); }

    @Scheduled(cron = "0 0 17 * * *", zone = "Asia/Kuala_Lumpur")
    public void captureEndOfDay() { snapshotService.createSnapshot(LocalDate.now(TaskSnapshotService.COMPANY_ZONE), SnapshotType.END_OF_DAY, false); }

    @Scheduled(cron = "0 */15 * * * *", zone = "Asia/Kuala_Lumpur")
    public void recoverMissedSnapshots() { recoveryService.recoverMissingForToday(); }
}
