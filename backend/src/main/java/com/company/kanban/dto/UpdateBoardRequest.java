package com.company.kanban.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateBoardRequest(

        @NotBlank
        String name,

        String description

) {
}
