package com.company.kanban.dto;

import com.company.kanban.entity.SnapshotBatch;
import com.company.kanban.entity.SnapshotBatchStatus;
import com.company.kanban.entity.SnapshotType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SnapshotBatchResponse(Long id, LocalDate snapshotDate, SnapshotType snapshotType,
                                    LocalDateTime scheduledFor, LocalDateTime capturedAt,
                                    boolean recovered, SnapshotBatchStatus status, int taskCount) {
    public static SnapshotBatchResponse from(SnapshotBatch batch) {
        return new SnapshotBatchResponse(batch.getId(), batch.getSnapshotDate(), batch.getSnapshotType(),
                batch.getScheduledFor(), batch.getCapturedAt(), batch.isRecovered(), batch.getStatus(), batch.getTaskCount());
    }
}
