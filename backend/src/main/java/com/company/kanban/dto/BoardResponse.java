package com.company.kanban.dto;

import java.time.LocalDateTime;

public record BoardResponse(

        Long id,
        String name,
        String description,
        Long departmentId,
        String departmentName,
        LocalDateTime createdAt

) {
}