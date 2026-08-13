package com.company.kanban.dto;

public record LoginResponse(
        String token,
        Long userId,
        String name,
        String email,
        String role,
        Long departmentId,
        String departmentName
) {
}