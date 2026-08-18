package com.company.kanban.service;

import com.company.kanban.dto.BoardResponse;
import com.company.kanban.dto.CreateBoardRequest;
import com.company.kanban.dto.UpdateBoardRequest;
import com.company.kanban.entity.Board;
import com.company.kanban.entity.Department;
import com.company.kanban.entity.KanbanColumn;
import com.company.kanban.entity.User;
import com.company.kanban.repository.BoardRepository;
import com.company.kanban.repository.DepartmentRepository;
import com.company.kanban.repository.KanbanColumnRepository;
import com.company.kanban.repository.TaskRepository;

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
    private final TaskRepository taskRepository;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;

    public BoardService(
            BoardRepository boardRepository,
            DepartmentRepository departmentRepository,
            KanbanColumnRepository kanbanColumnRepository,
            TaskRepository taskRepository,
            AuthorizationService authorizationService,
            NotificationService notificationService) {

        this.boardRepository = boardRepository;
        this.departmentRepository = departmentRepository;
        this.kanbanColumnRepository = kanbanColumnRepository;
        this.taskRepository = taskRepository;
        this.authorizationService = authorizationService;
        this.notificationService = notificationService;
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

        notificationService.notifyProjectCreated(savedBoard, currentUser);

        return toResponse(savedBoard);
    }

    @Transactional
    public BoardResponse updateBoard(
            Long boardId, UpdateBoardRequest request, User currentUser) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Board not found"
                ));

        authorizationService.requireBoardManagementAccess(
                currentUser,
                board.getDepartment().getId()
        );

        if (!board.getName().equalsIgnoreCase(request.name())
                && boardRepository.existsByNameIgnoreCaseAndDepartmentId(
                        request.name(), board.getDepartment().getId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A board with this name already exists in this department"
            );
        }

        board.setName(request.name());
        board.setDescription(request.description());

        return toResponse(board);
    }

    @Transactional
    public void deleteBoard(Long boardId, User currentUser) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Board not found"
                ));

        authorizationService.requireBoardManagementAccess(
                currentUser,
                board.getDepartment().getId()
        );

        if (taskRepository.existsByColumnBoardId(boardId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot delete board because it still contains tasks"
            );
        }

        // Columns are not configured with cascade delete. Remove the empty
        // columns first so deleting the board cannot leave orphaned rows or
        // fail on the foreign-key constraint.
        kanbanColumnRepository.deleteByBoardId(boardId);
        boardRepository.delete(board);
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
