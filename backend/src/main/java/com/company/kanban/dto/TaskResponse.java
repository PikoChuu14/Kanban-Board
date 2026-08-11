package com.company.kanban.dto;

import com.company.kanban.entity.Priority;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(

        Long id,
        String title,
        String description,
        Priority priority,
        LocalDate dueDate,
        Integer position,

        Long columnId,
        String columnName,

        Long assigneeId,
        String assigneeName,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
}