package com.company.kanban.controller;

import com.company.kanban.dto.BoardResponse;
import com.company.kanban.dto.CreateBoardRequest;
import com.company.kanban.service.BoardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    public List<BoardResponse> getAllBoards() {
        return boardService.getAllBoards();
    }

    @GetMapping("/{id}")
    public BoardResponse getBoardById(
            @PathVariable Long id) {

        return boardService.getBoardById(id);
    }

    @GetMapping("/department/{departmentId}")
    public List<BoardResponse> getBoardsByDepartment(
            @PathVariable Long departmentId) {

        return boardService
                .getBoardsByDepartment(departmentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BoardResponse createBoard(
            @Valid
            @RequestBody CreateBoardRequest request) {

        return boardService.createBoard(request);
    }
}
