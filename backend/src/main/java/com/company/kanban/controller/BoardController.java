package com.company.kanban.controller;

import com.company.kanban.dto.BoardResponse;
import com.company.kanban.dto.CreateBoardRequest;
import com.company.kanban.service.BoardService;
import com.company.kanban.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    public List<BoardResponse> getAllBoards(@AuthenticationPrincipal User currentUser) {
        return boardService.getAllBoards(currentUser);
    }

    @GetMapping("/{id}")
    public BoardResponse getBoardById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        return boardService.getBoardById(id, currentUser);
    }

    @GetMapping("/department/{departmentId}")
    public List<BoardResponse> getBoardsByDepartment(
            @PathVariable Long departmentId,
            @AuthenticationPrincipal User currentUser) {

        return boardService
                .getBoardsByDepartment(departmentId, currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BoardResponse createBoard(
            @Valid
            @RequestBody CreateBoardRequest request,
            @AuthenticationPrincipal User currentUser) {

        return boardService.createBoard(request, currentUser);
    }
}
