package com.company.kanban.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBoardRequest(

        @NotBlank
        String name,

        String description,

        @NotNull
        Long departmentId

) {
}