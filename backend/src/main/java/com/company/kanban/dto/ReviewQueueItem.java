package com.company.kanban.dto;

import com.company.kanban.entity.Priority;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReviewQueueItem(Long taskId, String title, String description, Integer workload, Priority priority,
                              LocalDate dueDate, Long assigneeId, String assigneeName, Long boardId,
                              String boardName, Long departmentId, String departmentName,
                              LocalDateTime submittedForReviewAt, LocalDateTime updatedAt) {}
