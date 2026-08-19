package com.company.kanban.controller;

import com.company.kanban.dto.LoginRequest;
import com.company.kanban.dto.LoginResponse;
import com.company.kanban.entity.User;
import com.company.kanban.service.AuthService;
import jakarta.validation.Valid;
import com.company.kanban.dto.ActivateAccountRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final com.company.kanban.service.ActivationService activationService;

    public AuthController(AuthService authService, com.company.kanban.service.ActivationService activationService) {
        this.authService = authService;
        this.activationService = activationService;
    }

    @PostMapping("/activate")
    public void activate(@Valid @RequestBody ActivateAccountRequest request) { activationService.activate(request.token(), request.password()); }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public LoginResponse me(
            @AuthenticationPrincipal User user
    ) {
        return new LoginResponse(
                null,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getDepartment().getId(),
                user.getDepartment().getName()
        );
    }
}
