package com.company.kanban.dto;

import java.time.LocalDateTime;
public record ArchiveResponse(String databaseName, LocalDateTime archivedAt, String reason, String matchingBackup, String status) {}
