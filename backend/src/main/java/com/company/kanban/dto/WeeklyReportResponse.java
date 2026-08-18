package com.company.kanban.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record WeeklyReportResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        Long departmentId,
        String departmentName,
        Overview overview,
        List<Day> days) {
    public record Overview(int staff, int dailyReportsSubmitted, int draftReports, int notStarted,
                           int tasksCompleted, int tasksMovedToReview, Integer averageActiveWorkload,
                           Integer weekStartWorkload, Integer weekEndWorkload, boolean weekEndCurrent) {}
    public record Day(LocalDate date, List<StaffDay> staff) {}
    public record StaffDay(Long userId, String userName, String status, String workSummary,
                           String blockers, String nextDayPlan, LocalDateTime submittedAt,
                           Snapshot start, Snapshot end, String rightHandSource,
                           List<Task> tasks, int completedCount, int reviewCount) {}
    public record Snapshot(Integer draft, Integer doing, Integer review, Integer done,
                           Integer workload) {}
    public record Task(Long taskId, String title, String status, Integer workload, String boardName) {}
}
