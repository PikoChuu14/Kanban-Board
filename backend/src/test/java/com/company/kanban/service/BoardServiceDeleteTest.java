package com.company.kanban.service;

import com.company.kanban.entity.Board;
import com.company.kanban.entity.Department;
import com.company.kanban.entity.Role;
import com.company.kanban.entity.User;
import com.company.kanban.repository.BoardRepository;
import com.company.kanban.repository.DepartmentRepository;
import com.company.kanban.repository.KanbanColumnRepository;
import com.company.kanban.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BoardServiceDeleteTest {

    private final BoardRepository boardRepository = mock(BoardRepository.class);
    private final KanbanColumnRepository kanbanColumnRepository = mock(KanbanColumnRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final BoardService boardService = new BoardService(
            boardRepository,
            mock(DepartmentRepository.class),
            kanbanColumnRepository,
            taskRepository,
            new AuthorizationService(),
            mock(NotificationService.class)
    );

    @Test
    void adminDeletesEmptyBoard() {
        Board board = boardInDepartment(10L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(taskRepository.existsByColumnBoardId(1L)).thenReturn(false);

        assertDoesNotThrow(() -> boardService.deleteBoard(1L, user(Role.ADMIN, null)));

        verify(kanbanColumnRepository).deleteByBoardId(1L);
        verify(boardRepository).delete(board);
    }

    @Test
    void managerDeletesEmptyBoardInOwnDepartment() {
        Board board = boardInDepartment(10L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(taskRepository.existsByColumnBoardId(1L)).thenReturn(false);

        assertDoesNotThrow(() -> boardService.deleteBoard(1L, user(Role.MANAGER, 10L)));

        verify(kanbanColumnRepository).deleteByBoardId(1L);
        verify(boardRepository).delete(board);
    }

    @Test
    void managerCannotDeleteBoardInAnotherDepartment() {
        Board board = boardInDepartment(10L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> boardService.deleteBoard(1L, user(Role.MANAGER, 20L))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(boardRepository, never()).delete(board);
    }

    @Test
    void staffCannotDeleteAnyBoard() {
        Board board = boardInDepartment(10L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> boardService.deleteBoard(1L, user(Role.STAFF, 10L))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(boardRepository, never()).delete(board);
    }

    @Test
    void cannotDeleteBoardContainingTasks() {
        Board board = boardInDepartment(10L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(taskRepository.existsByColumnBoardId(1L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> boardService.deleteBoard(1L, user(Role.ADMIN, null))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(kanbanColumnRepository, never()).deleteByBoardId(1L);
        verify(boardRepository, never()).delete(board);
    }

    private Board boardInDepartment(Long departmentId) {
        Board board = mock(Board.class);
        Department department = mock(Department.class);
        when(department.getId()).thenReturn(departmentId);
        when(board.getDepartment()).thenReturn(department);
        return board;
    }

    private User user(Role role, Long departmentId) {
        User user = mock(User.class);
        when(user.getRole()).thenReturn(role);

        if (departmentId != null) {
            Department department = mock(Department.class);
            when(department.getId()).thenReturn(departmentId);
            when(user.getDepartment()).thenReturn(department);
        }

        return user;
    }
}
