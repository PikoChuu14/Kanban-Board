package com.company.kanban.controller;

import com.company.kanban.dto.CreateUserRequest;
import com.company.kanban.dto.UserResponse;
import com.company.kanban.entity.User;
import com.company.kanban.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/assignable")
    @PreAuthorize("isAuthenticated()")
    public List<UserResponse> getAssignableUsers(
            @AuthenticationPrincipal User currentUser
    ) {
        return userService.getAssignableUsers(currentUser);
    }

    @GetMapping("/task-assignees")
    @PreAuthorize("isAuthenticated()")
    public List<UserResponse> getTaskAssignees(
            @AuthenticationPrincipal User currentUser
    ) {
        return userService.getTaskAssignees(currentUser);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @RequestBody CreateUserRequest request) {

        return userService.createUser(request);
    }
}
