package com.company.kanban.service;

import com.company.kanban.dto.CreateDepartmentRequest;
import com.company.kanban.entity.Department;
import com.company.kanban.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DepartmentServiceTest {
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final DepartmentService service = new DepartmentService(departments);

    @Test
    void adminCanCreateTrimmedDepartmentName() {
        when(departments.findByNameIgnoreCase("Facilities")).thenReturn(Optional.empty());
        when(departments.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createDepartment(new CreateDepartmentRequest("  Facilities  "));

        assertEquals("Facilities", result.name());
        verify(departments).save(any(Department.class));
    }

    @Test
    void duplicateDepartmentNameIsRejectedIgnoringCase() {
        when(departments.findByNameIgnoreCase("maintenance")).thenReturn(Optional.of(new Department("Maintenance")));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.createDepartment(new CreateDepartmentRequest("maintenance"))
        );

        assertEquals(409, error.getStatusCode().value());
    }
}
