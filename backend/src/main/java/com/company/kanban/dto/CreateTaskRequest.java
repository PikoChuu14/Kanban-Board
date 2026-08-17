package com.company.kanban.dto;

import com.company.kanban.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDate;

public record CreateTaskRequest(

        @NotBlank
        String title,

        String description,

        @NotNull
        Priority priority,

        LocalDate dueDate,

        @NotNull
        Long columnId,

        Long assigneeId,

        @NotNull
        @Min(1)
        @Max(5)
        Integer workload

) {
}
