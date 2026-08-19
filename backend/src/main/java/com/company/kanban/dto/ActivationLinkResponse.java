package com.company.kanban.dto;

import java.time.LocalDateTime;
public record ActivationLinkResponse(String activationLink, LocalDateTime expiresAt) {}
