package com.company.kanban.service;

import com.company.kanban.dto.CreateDepartmentRequest;
import com.company.kanban.dto.DepartmentResponse;
import com.company.kanban.repository.DepartmentRepository;
import com.company.kanban.entity.User;
import com.company.kanban.entity.Department;
import com.company.kanban.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository) {

        this.departmentRepository = departmentRepository;
    }

    public List<DepartmentResponse> getAllDepartments(User currentUser) {

        List<Department> departments = currentUser.getRole() == Role.ADMIN
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

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        String name = request.name().trim();
        if (departmentRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department already exists");
        }

        Department saved = departmentRepository.save(new Department(name));
        log.info("ADMIN action: department created id={} name={}", saved.getId(), saved.getName());
        return new DepartmentResponse(saved.getId(), saved.getName());
    }
}
