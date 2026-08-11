package com.company.kanban.service;

import com.company.kanban.dto.KanbanColumnResponse;
import com.company.kanban.repository.KanbanColumnRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KanbanColumnService {

    private final KanbanColumnRepository kanbanColumnRepository;

    public KanbanColumnService(
            KanbanColumnRepository kanbanColumnRepository) {

        this.kanbanColumnRepository = kanbanColumnRepository;
    }

    public List<KanbanColumnResponse> getColumnsByBoard(Long boardId) {

        return kanbanColumnRepository
                .findByBoardIdOrderByPositionAsc(boardId)
                .stream()
                .map(column ->
                        new KanbanColumnResponse(
                                column.getId(),
                                column.getName(),
                                column.getPosition(),
                                column.getBoard().getId()
                        )
                )
                .toList();
    }
}