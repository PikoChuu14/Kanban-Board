package com.company.kanban.dto;

import com.company.kanban.entity.AccountStatus;
import com.company.kanban.entity.Role;

public record UpdateUserRequest(String name, Long departmentId, Role role, AccountStatus status) {}
