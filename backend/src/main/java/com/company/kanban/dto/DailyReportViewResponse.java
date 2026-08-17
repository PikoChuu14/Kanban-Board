package com.company.kanban.dto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
public record DailyReportViewResponse(Employee employee, Report report, SnapshotSummary startOfDay, SnapshotSummary endOfDay, SnapshotSummary rightHandState, String rightHandSource, String comparisonLabel, List<TaskItem> completedTasks, List<TaskItem> reviewTasks, List<TaskItem> activeTasks, String startNote, String endNote) {
 public record Employee(Long userId, String userName, String role, Long departmentId, String departmentName) {}
 public record Report(LocalDate reportDate, String reportStatus, String workSummary, String blockers, String nextDayPlan, LocalDateTime submittedAt) {}
 public record SnapshotSummary(int draftCount, int doingCount, int reviewCount, int doneCount, int activeWorkload, LocalDateTime capturedAt, boolean recovered) {}
 public record TaskItem(Long taskId, String title, String status, Integer workload, String boardName) {}
}
