package com.company.kanban.service;

import com.company.kanban.entity.Board;
import com.company.kanban.entity.KanbanColumn;
import com.company.kanban.entity.Role;
import com.company.kanban.entity.Task;
import com.company.kanban.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
public class AuthorizationService {

    public boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    public boolean canAccessDepartment(User user, Long departmentId) {
        return isAdmin(user)
                || user != null
                && user.getDepartment() != null
                && Objects.equals(user.getDepartment().getId(), departmentId);
    }

    public void requireDepartmentAccess(User user, Long departmentId) {
        if (!canAccessDepartment(user, departmentId)) {
            throw forbidden();
        }
    }

    public void requireBoardAccess(User user, Board board) {
        requireDepartmentAccess(user, board.getDepartment().getId());
    }

    public void requireColumnAccess(User user, KanbanColumn column) {
        requireBoardAccess(user, column.getBoard());
    }

    public void requireTaskAccess(User user, Task task) {
        requireColumnAccess(user, task.getColumn());
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this department's data"
        );
    }
}
