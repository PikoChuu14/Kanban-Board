package com.company.kanban.service;

import com.company.kanban.entity.Department;
import com.company.kanban.entity.Role;
import com.company.kanban.entity.User;
import org.junit.jupiter.api.Test;

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
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                authorizationService.requireDepartmentAccess(manager, 20L));
        assertFalse(authorizationService.canAccessDepartment(manager, 20L));
    }
}
