package com.company.kanban.service;

import com.company.kanban.entity.Board;
import com.company.kanban.entity.Department;
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

    public void requireAssignableUser(User currentUser, User assignee) {
        if (isAdmin(currentUser)) {
            return;
        }

        if (currentUser == null
                || currentUser.getDepartment() == null
                || assignee == null
                || assignee.getDepartment() == null
                || !currentUser.getDepartment().getId()
                .equals(assignee.getDepartment().getId())) {

            throw forbidden();
        }
    }

    public void requireAssigneeMatchesTaskDepartment(
            User assignee,
            Department taskDepartment) {

        if (assignee == null) {
            return;
        }

        if (taskDepartment == null
                || assignee.getDepartment() == null
                || !assignee.getDepartment().getId()
                .equals(taskDepartment.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Assignee must belong to the task's department"
            );
        }
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this department's data"
        );
    }
}
