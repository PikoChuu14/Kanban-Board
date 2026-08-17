package com.company.kanban.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TeamDailyReportsResponse(
        LocalDate date,
        Long departmentId,
        String departmentName,
        int submittedCount,
        int draftCount,
        int notStartedCount,
        List<ReportCard> reports) {
    public record ReportCard(
            Long userId,
            String userName,
            String status,
            String workSummary,
            String blockers,
            String nextDayPlan,
            LocalDateTime submittedAt,
            int completedCount,
            int reviewCount,
            int doingCount,
            int activeWorkload,
            Integer startDraft,
            Integer startDoing,
            Integer startReview,
            Integer startDone,
            Integer startWorkload,
            Integer rightDraft,
            Integer rightDoing,
            Integer rightReview,
            Integer rightDone,
            Integer rightWorkload,
            String rightHandSource,
            String comparisonLabel,
            List<String> completedTasks,
            List<String> reviewTasks,
            List<String> activeTasks) {}
}
