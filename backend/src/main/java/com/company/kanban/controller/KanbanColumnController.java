package com.company.kanban.controller;

import com.company.kanban.dto.KanbanColumnResponse;
import com.company.kanban.service.KanbanColumnService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/columns")
public class KanbanColumnController {

    private final KanbanColumnService kanbanColumnService;

    public KanbanColumnController(
            KanbanColumnService kanbanColumnService) {

        this.kanbanColumnService = kanbanColumnService;
    }

    @GetMapping("/board/{boardId}")
    public List<KanbanColumnResponse> getColumnsByBoard(
            @PathVariable Long boardId) {

        return kanbanColumnService.getColumnsByBoard(boardId);
    }
}