package com.company.kanban.service;

import com.company.kanban.dto.KanbanColumnResponse;
import com.company.kanban.repository.KanbanColumnRepository;
import com.company.kanban.repository.BoardRepository;
import com.company.kanban.entity.User;
import com.company.kanban.entity.Board;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KanbanColumnService {

    private final KanbanColumnRepository kanbanColumnRepository;
    private final BoardRepository boardRepository;
    private final AuthorizationService authorizationService;

    public KanbanColumnService(
            KanbanColumnRepository kanbanColumnRepository,
            BoardRepository boardRepository,
            AuthorizationService authorizationService) {

        this.kanbanColumnRepository = kanbanColumnRepository;
        this.boardRepository = boardRepository;
        this.authorizationService = authorizationService;
    }

    public List<KanbanColumnResponse> getColumnsByBoard(Long boardId, User currentUser) {

        Board board = boardRepository.findById(boardId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found"));
        authorizationService.requireBoardAccess(currentUser, board);

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
