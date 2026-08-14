package com.company.kanban.controller;

import com.company.kanban.dto.StaffWorkloadResponse;
import com.company.kanban.entity.User;
import com.company.kanban.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/team-workload")
    public List<StaffWorkloadResponse> getTeamWorkload(
            @RequestParam(required = false) Long departmentId,
            @AuthenticationPrincipal User currentUser) {
        return dashboardService.getTeamWorkload(currentUser, departmentId);
    }
}
