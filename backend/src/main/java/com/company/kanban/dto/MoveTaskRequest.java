package com.company.kanban.dto;

import jakarta.validation.constraints.NotNull;

public record MoveTaskRequest(

        @NotNull
        Long targetColumnId,

        @NotNull
        Integer targetPosition

) {
}