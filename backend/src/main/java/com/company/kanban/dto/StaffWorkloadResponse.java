package com.company.kanban.dto;

public record StaffWorkloadResponse(
        Long userId,
        String name,
        String email,
        Long departmentId,
        String departmentName,
        Integer totalWorkload,
        Long activeTaskCount,
        Long draftCount,
        Long doingCount,
        Long reviewCount,
        Long doneCount
) {
}
