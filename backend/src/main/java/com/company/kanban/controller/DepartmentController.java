package com.company.kanban.controller;

import com.company.kanban.dto.DepartmentResponse;
import com.company.kanban.service.DepartmentService;
import com.company.kanban.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(
            DepartmentService departmentService) {

        this.departmentService = departmentService;
    }

    @GetMapping
    public List<DepartmentResponse> getAllDepartments(
            @AuthenticationPrincipal User currentUser) {
        return departmentService.getAllDepartments(currentUser);
    }
}
