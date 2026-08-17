package com.company.kanban.dto;

import java.time.LocalDate;

public record SnapshotDateResponse(LocalDate date, boolean hasStartOfDay, boolean hasEndOfDay) {}
