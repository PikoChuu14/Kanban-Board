package com.company.kanban.service;

import com.company.kanban.dto.BoardResponse;
import com.company.kanban.dto.CreateBoardRequest;
import com.company.kanban.entity.Board;
import com.company.kanban.entity.Department;
import com.company.kanban.entity.KanbanColumn;
import com.company.kanban.entity.User;
import com.company.kanban.repository.BoardRepository;
import com.company.kanban.repository.DepartmentRepository;
import com.company.kanban.repository.KanbanColumnRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final DepartmentRepository departmentRepository;
    private final KanbanColumnRepository kanbanColumnRepository;
    private final AuthorizationService authorizationService;

    public BoardService(
            BoardRepository boardRepository,
            DepartmentRepository departmentRepository,
            KanbanColumnRepository kanbanColumnRepository,
            AuthorizationService authorizationService) {

        this.boardRepository = boardRepository;
        this.departmentRepository = departmentRepository;
        this.kanbanColumnRepository = kanbanColumnRepository;
        this.authorizationService = authorizationService;
    }

    public List<BoardResponse> getAllBoards(User currentUser) {

        List<Board> boards = authorizationService.isAdmin(currentUser)
                ? boardRepository.findAll()
                : boardRepository.findByDepartmentId(currentUser.getDepartment().getId());
        return boards
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BoardResponse getBoardById(Long id, User currentUser) {

        Board board = boardRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Board not found"
                        )
                );
        authorizationService.requireBoardAccess(currentUser, board);

        return toResponse(board);
    }

    public List<BoardResponse> getBoardsByDepartment(
            Long departmentId, User currentUser) {

        authorizationService.requireDepartmentAccess(currentUser, departmentId);

        return boardRepository
                .findByDepartmentId(departmentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

        @Transactional
        public BoardResponse createBoard(
            CreateBoardRequest request, User currentUser) {

        Department department =
                departmentRepository
                        .findById(request.departmentId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Department not found"
                                )
                        );

        authorizationService.requireBoardManagementAccess(
                currentUser,
                department.getId()
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

        kanbanColumnRepository.save(
                new KanbanColumn("To Do", 1, savedBoard)
        );

        kanbanColumnRepository.save(
                new KanbanColumn("In Progress", 2, savedBoard)
        );

        kanbanColumnRepository.save(
                new KanbanColumn("Review", 3, savedBoard)
        );

        kanbanColumnRepository.save(
                new KanbanColumn("Done", 4, savedBoard)
        );

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
