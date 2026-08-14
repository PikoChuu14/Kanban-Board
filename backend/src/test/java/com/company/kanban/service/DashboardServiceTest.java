package com.company.kanban.service;

import com.company.kanban.entity.Department;
import com.company.kanban.entity.Role;
import com.company.kanban.entity.Task;
import com.company.kanban.entity.TaskStatus;
import com.company.kanban.entity.User;
import com.company.kanban.repository.TaskRepository;
import com.company.kanban.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final DashboardService dashboardService = new DashboardService(
            userRepository, taskRepository, new AuthorizationService());

    @Test
    void sumsOnlyUnfinishedTaskWorkloadAndCountsStatuses() {
        User staff = user(2L, "John", "PPC", 10L, Role.STAFF);
        when(userRepository.findByDepartmentIdOrderByNameAsc(10L)).thenReturn(List.of(staff));
        List<Task> tasks = List.of(
                task(TaskStatus.DRAFT, 2),
                task(TaskStatus.DOING, 4),
                task(TaskStatus.REVIEW, 3),
                task(TaskStatus.DONE, 10));
        when(taskRepository.findByAssigneeIdOrderByStatusAscPositionAsc(2L)).thenReturn(tasks);

        var response = dashboardService.getTeamWorkload(
                user(1L, "Manager", "PPC", 10L, Role.MANAGER));

        assertEquals(9, response.get(0).totalWorkload());
        assertEquals(1L, response.get(0).draftCount());
        assertEquals(1L, response.get(0).doingCount());
        assertEquals(1L, response.get(0).reviewCount());
        assertEquals(1L, response.get(0).doneCount());
    }

    @Test
    void staffCannotAccessWorkloadDashboard() {
        assertThrows(ResponseStatusException.class, () -> dashboardService.getTeamWorkload(
                user(3L, "Staff", "PPC", 10L, Role.STAFF)));
    }

    @Test
    void managerUsesOwnDepartmentAndAdminUsesAllUsers() {
        User manager = user(1L, "Manager", "PPC", 10L, Role.MANAGER);
        User admin = user(4L, "Admin", "Admin", 99L, Role.ADMIN);
        when(userRepository.findByDepartmentIdOrderByNameAsc(10L)).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        dashboardService.getTeamWorkload(manager);
        dashboardService.getTeamWorkload(admin);

        org.mockito.Mockito.verify(userRepository).findByDepartmentIdOrderByNameAsc(10L);
        org.mockito.Mockito.verify(userRepository).findAll();
    }

    private User user(Long id, String name, String departmentName, Long departmentId, Role role) {
        Department department = mock(Department.class);
        when(department.getId()).thenReturn(departmentId);
        when(department.getName()).thenReturn(departmentName);
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn(name);
        when(user.getDepartment()).thenReturn(department);
        when(user.getRole()).thenReturn(role);
        return user;
    }

    private Task task(TaskStatus status, Integer workload) {
        Task task = mock(Task.class);
        when(task.getStatus()).thenReturn(status);
        when(task.getWorkload()).thenReturn(workload);
        return task;
    }
}
