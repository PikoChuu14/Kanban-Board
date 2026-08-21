package com.company.kanban.service;

import com.company.kanban.dto.UserResponse;
import com.company.kanban.entity.AccountStatus;
import com.company.kanban.entity.Department;
import com.company.kanban.entity.Role;
import com.company.kanban.entity.User;
import com.company.kanban.repository.ActivationTokenRepository;
import com.company.kanban.repository.DailyWorkReportRepository;
import com.company.kanban.repository.DepartmentRepository;
import com.company.kanban.repository.NotificationRepository;
import com.company.kanban.repository.TaskRepository;
import com.company.kanban.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTaskAssigneesTest {

    private final UserRepository users = mock(UserRepository.class);
    private final UserService service = new UserService(
            users,
            mock(DepartmentRepository.class),
            mock(PasswordEncoder.class),
            mock(ActivationTokenRepository.class),
            mock(TaskRepository.class),
            mock(DailyWorkReportRepository.class),
            mock(NotificationRepository.class)
    );

    @Test
    void managerCanAssignTaskToSelfButNotAnotherManager() {
        Department department = department(10L, "PPC");
        User currentManager = user(1L, "Current Manager", Role.MANAGER, department);
        User otherManager = user(2L, "Other Manager", Role.MANAGER, department);
        User staff = user(3L, "Staff Member", Role.STAFF, department);

        when(users.findByDepartmentIdOrderByNameAsc(10L))
                .thenReturn(List.of(currentManager, otherManager, staff));

        List<UserResponse> assignees = service.getTaskAssignees(currentManager);

        assertEquals(List.of(1L, 3L), assignees.stream().map(UserResponse::id).toList());
    }

    @Test
    void disabledCurrentManagerIsNotAssignable() {
        Department department = department(10L, "PPC");
        User currentManager = user(1L, "Current Manager", Role.MANAGER, department);
        currentManager.setStatus(AccountStatus.DISABLED);

        when(users.findByDepartmentIdOrderByNameAsc(10L)).thenReturn(List.of(currentManager));

        assertEquals(List.of(), service.getTaskAssignees(currentManager));
    }

    private static Department department(Long id, String name) {
        Department department = new Department(name);
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }

    private static User user(Long id, String name, Role role, Department department) {
        User user = new User(name, name.toLowerCase().replace(' ', '.') + "@example.test", "hash", role, department);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
