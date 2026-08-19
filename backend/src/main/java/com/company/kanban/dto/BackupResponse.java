package com.company.kanban.dto;

import java.time.LocalDateTime;
public record BackupResponse(String id, String filename, LocalDateTime createdAt, long sizeBytes,
        String backupType, String reason, String sourceDatabase, String status) {}
