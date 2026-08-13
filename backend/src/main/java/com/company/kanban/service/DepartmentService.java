package com.company.kanban.service;

import com.company.kanban.dto.DepartmentResponse;
import com.company.kanban.repository.DepartmentRepository;
import com.company.kanban.entity.User;
import com.company.kanban.entity.Department;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository) {

        this.departmentRepository = departmentRepository;
    }

    public List<DepartmentResponse> getAllDepartments(User currentUser) {

        List<Department> departments = currentUser.getRole().name().equals("ADMIN")
                ? departmentRepository.findAll()
                : List.of(currentUser.getDepartment());
        return departments
                .stream()
                .map(department ->
                        new DepartmentResponse(
                                department.getId(),
                                department.getName()
                        )
                )
                .toList();
    }
}
