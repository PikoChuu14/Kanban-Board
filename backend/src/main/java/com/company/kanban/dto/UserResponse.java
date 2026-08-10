package com.company.kanban.dto;

import com.company.kanban.entity.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        Long departmentId,
        String departmentName,
        LocalDateTime createdAt
) {
}