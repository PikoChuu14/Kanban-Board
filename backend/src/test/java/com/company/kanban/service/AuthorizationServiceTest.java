package com.company.kanban.service;

import com.company.kanban.entity.Department;
import com.company.kanban.entity.Role;
import com.company.kanban.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationServiceTest {

    private final AuthorizationService authorizationService = new AuthorizationService();

    @Test
    void adminCanAccessAnyDepartment() {
        User admin = mock(User.class);
        when(admin.getRole()).thenReturn(Role.ADMIN);

        assertDoesNotThrow(() ->
                authorizationService.requireDepartmentAccess(admin, 999L));
    }

    @Test
    void nonAdminCanAccessOnlyOwnDepartment() {
        Department department = mock(Department.class);
        when(department.getId()).thenReturn(10L);

        User manager = mock(User.class);
        when(manager.getRole()).thenReturn(Role.MANAGER);
        when(manager.getDepartment()).thenReturn(department);

        assertDoesNotThrow(() ->
                authorizationService.requireDepartmentAccess(manager, 10L));
        assertThrows(ResponseStatusException.class, () ->
                authorizationService.requireDepartmentAccess(manager, 20L));
        assertFalse(authorizationService.canAccessDepartment(manager, 20L));
    }

    @Test
    void onlyAdminOrSameDepartmentManagerCanManageBoards() {
        Department department = mock(Department.class);
        when(department.getId()).thenReturn(10L);

        User admin = mock(User.class);
        when(admin.getRole()).thenReturn(Role.ADMIN);

        User manager = mock(User.class);
        when(manager.getRole()).thenReturn(Role.MANAGER);
        when(manager.getDepartment()).thenReturn(department);

        User staff = mock(User.class);
        when(staff.getRole()).thenReturn(Role.STAFF);
        when(staff.getDepartment()).thenReturn(department);

        assertDoesNotThrow(() ->
                authorizationService.requireBoardManagementAccess(admin, 20L));
        assertDoesNotThrow(() ->
                authorizationService.requireBoardManagementAccess(manager, 10L));
        assertThrows(ResponseStatusException.class, () ->
                authorizationService.requireBoardManagementAccess(manager, 20L));
        assertThrows(ResponseStatusException.class, () ->
                authorizationService.requireBoardManagementAccess(staff, 10L));
    }

    @Test
    void adminCanAssignAnyUser() {
        User admin = mock(User.class);
        when(admin.getRole()).thenReturn(Role.ADMIN);

        User assignee = mock(User.class);

        assertDoesNotThrow(() ->
                authorizationService.requireAssignableUser(admin, assignee));
    }

    @Test
    void nonAdminCannotAssignUserFromAnotherDepartment() {
        Department currentDepartment = mock(Department.class);
        when(currentDepartment.getId()).thenReturn(10L);

        Department assigneeDepartment = mock(Department.class);
        when(assigneeDepartment.getId()).thenReturn(20L);

        User manager = mock(User.class);
        when(manager.getRole()).thenReturn(Role.MANAGER);
        when(manager.getDepartment()).thenReturn(currentDepartment);

        User assignee = mock(User.class);
        when(assignee.getDepartment()).thenReturn(assigneeDepartment);

        assertThrows(ResponseStatusException.class, () ->
                authorizationService.requireAssignableUser(manager, assignee));
    }

    @Test
    void staffCanAssignSameDepartmentStaffButNotManagers() {
        Department department = mock(Department.class);
        when(department.getId()).thenReturn(10L);

        User staff = mock(User.class);
        when(staff.getRole()).thenReturn(Role.STAFF);
        when(staff.getDepartment()).thenReturn(department);

        User teammate = mock(User.class);
        when(teammate.getRole()).thenReturn(Role.STAFF);
        when(teammate.getDepartment()).thenReturn(department);
        assertDoesNotThrow(() -> authorizationService.requireAssignableUser(staff, teammate));

        User manager = mock(User.class);
        when(manager.getRole()).thenReturn(Role.MANAGER);
        when(manager.getDepartment()).thenReturn(department);
        assertThrows(ResponseStatusException.class, () ->
                authorizationService.requireAssignableUser(staff, manager));
    }

    @Test
    void assigneeMustMatchTaskDepartment() {
        Department taskDepartment = mock(Department.class);
        when(taskDepartment.getId()).thenReturn(10L);

        Department assigneeDepartment = mock(Department.class);
        when(assigneeDepartment.getId()).thenReturn(20L);

        User assignee = mock(User.class);
        when(assignee.getDepartment()).thenReturn(assigneeDepartment);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authorizationService.requireAssigneeMatchesTaskDepartment(
                        assignee,
                        taskDepartment)
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }
}
