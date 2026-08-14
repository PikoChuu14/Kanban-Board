package com.company.kanban.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewActionRequest(

        @NotNull
        ReviewAction action

) {
}
