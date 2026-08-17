package com.company.kanban.dto;

import com.company.kanban.entity.TaskStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(

        @NotNull
        TaskStatus status,

        @NotNull
        @Min(1)
        Integer targetPosition

) {
}
