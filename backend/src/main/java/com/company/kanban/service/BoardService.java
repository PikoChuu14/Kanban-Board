package com.company.kanban.service;

import com.company.kanban.dto.BoardResponse;
import com.company.kanban.dto.CreateBoardRequest;
import com.company.kanban.entity.Board;
import com.company.kanban.entity.Department;
import com.company.kanban.repository.BoardRepository;
import com.company.kanban.repository.DepartmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final DepartmentRepository departmentRepository;

    public BoardService(
            BoardRepository boardRepository,
            DepartmentRepository departmentRepository) {

        this.boardRepository = boardRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<BoardResponse> getAllBoards() {

        return boardRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BoardResponse getBoardById(Long id) {

        Board board = boardRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Board not found"
                        )
                );

        return toResponse(board);
    }

    public List<BoardResponse> getBoardsByDepartment(
            Long departmentId) {

        return boardRepository
                .findByDepartmentId(departmentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BoardResponse createBoard(
            CreateBoardRequest request) {

        Department department =
                departmentRepository
                        .findById(request.departmentId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Department not found"
                                )
                        );

        if (boardRepository
                .existsByNameIgnoreCaseAndDepartmentId(
                        request.name(),
                        request.departmentId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A board with this name already exists in this department"
            );
        }

        Board board = new Board(
                request.name(),
                request.description(),
                department
        );

        Board savedBoard =
                boardRepository.save(board);

        return toResponse(savedBoard);
    }

    private BoardResponse toResponse(Board board) {

        return new BoardResponse(
                board.getId(),
                board.getName(),
                board.getDescription(),
                board.getDepartment().getId(),
                board.getDepartment().getName(),
                board.getCreatedAt()
        );
    }
}
