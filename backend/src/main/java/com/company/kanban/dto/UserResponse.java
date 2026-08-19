package com.company.kanban.dto;

import com.company.kanban.entity.Role;
import com.company.kanban.entity.AccountStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        Long departmentId,
        String departmentName,
        AccountStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
