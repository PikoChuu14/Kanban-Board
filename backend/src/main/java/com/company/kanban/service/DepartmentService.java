package com.company.kanban.service;

import com.company.kanban.dto.DepartmentResponse;
import com.company.kanban.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository) {

        this.departmentRepository = departmentRepository;
    }

    public List<DepartmentResponse> getAllDepartments() {

        return departmentRepository.findAll()
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