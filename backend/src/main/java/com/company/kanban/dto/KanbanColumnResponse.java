package com.company.kanban.dto;

public record KanbanColumnResponse(
        Long id,
        String name,
        Integer position,
        Long boardId
) {
}