package com.company.kanban.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "snapshot_batches", uniqueConstraints = @UniqueConstraint(
        name = "uk_snapshot_batch_date_type",
        columnNames = {"snapshot_date", "snapshot_type"}
))
public class SnapshotBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_type", nullable = false)
    private SnapshotType snapshotType;

    @Column(nullable = false)
    private LocalDateTime scheduledFor;

    private LocalDateTime capturedAt;

    @Column(nullable = false)
    private boolean recovered;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SnapshotBatchStatus status;

    @Column(nullable = false)
    private int taskCount;

    @Column(length = 2000)
    private String errorMessage;

    protected SnapshotBatch() {}

    public SnapshotBatch(LocalDate snapshotDate, SnapshotType snapshotType,
                          LocalDateTime scheduledFor, boolean recovered) {
        this.snapshotDate = snapshotDate;
        this.snapshotType = snapshotType;
        this.scheduledFor = scheduledFor;
        this.recovered = recovered;
        this.status = SnapshotBatchStatus.FAILED;
    }

    public Long getId() { return id; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public SnapshotType getSnapshotType() { return snapshotType; }
    public LocalDateTime getScheduledFor() { return scheduledFor; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public boolean isRecovered() { return recovered; }
    public SnapshotBatchStatus getStatus() { return status; }
    public int getTaskCount() { return taskCount; }
    public String getErrorMessage() { return errorMessage; }

    public void complete(LocalDateTime capturedAt, int taskCount) {
        this.capturedAt = capturedAt;
        this.taskCount = taskCount;
        this.status = SnapshotBatchStatus.COMPLETED;
        this.errorMessage = null;
    }

    public void fail(LocalDateTime capturedAt, String errorMessage) {
        this.capturedAt = capturedAt;
        this.status = SnapshotBatchStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
