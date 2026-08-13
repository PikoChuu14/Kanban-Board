package com.company.kanban.service;

import com.company.kanban.dto.UpdateBoardRequest;
import com.company.kanban.entity.Board;
import com.company.kanban.entity.Department;
import com.company.kanban.entity.Role;
import com.company.kanban.entity.User;
import com.company.kanban.repository.BoardRepository;
import com.company.kanban.repository.DepartmentRepository;
import com.company.kanban.repository.KanbanColumnRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BoardServiceUpdateTest {

    private final BoardRepository boardRepository = mock(BoardRepository.class);
    private final BoardService boardService = new BoardService(
            boardRepository,
            mock(DepartmentRepository.class),
            mock(KanbanColumnRepository.class),
            new AuthorizationService()
    );

    @Test
    void adminCanUpdateAnyBoard() {
        Board board = boardInDepartment(10L);
        User admin = user(Role.ADMIN, null);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        assertDoesNotThrow(() -> boardService.updateBoard(
                1L, new UpdateBoardRequest("Updated QC", "Updated description"), admin));

        verify(board).setName("Updated QC");
        verify(board).setDescription("Updated description");
    }

    @Test
    void managerCanUpdateBoardInOwnDepartment() {
        Board board = boardInDepartment(10L);
        User manager = user(Role.MANAGER, 10L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        assertDoesNotThrow(() -> boardService.updateBoard(
                1L, new UpdateBoardRequest("Updated QC", "Updated description"), manager));

        verify(board).setName("Updated QC");
    }

    @Test
    void managerCannotUpdateBoardInAnotherDepartment() {
        Board board = boardInDepartment(10L);
        User manager = user(Role.MANAGER, 20L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> boardService.updateBoard(
                        1L, new UpdateBoardRequest("Updated QC", "Updated description"), manager)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(board, never()).setName(any());
    }

    @Test
    void staffCannotUpdateAnyBoard() {
        Board board = boardInDepartment(10L);
        User staff = user(Role.STAFF, 10L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> boardService.updateBoard(
                        1L, new UpdateBoardRequest("Updated QC", "Updated description"), staff)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(board, never()).setName(any());
    }

    private Board boardInDepartment(Long departmentId) {
        Board board = mock(Board.class);
        Department department = mock(Department.class);
        when(department.getId()).thenReturn(departmentId);
        when(board.getDepartment()).thenReturn(department);
        when(board.getName()).thenReturn("QC Board");
        when(boardRepository.existsByNameIgnoreCaseAndDepartmentId(any(), anyLong()))
                .thenReturn(false);
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
