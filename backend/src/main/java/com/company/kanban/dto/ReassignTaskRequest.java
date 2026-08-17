package com.company.kanban.dto;

import jakarta.validation.constraints.NotNull;

public record ReassignTaskRequest(
        @NotNull(message = "assigneeId is required")
        Long assigneeId
) {
}
