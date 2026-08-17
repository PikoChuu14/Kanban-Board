package com.company.kanban.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_work_reports", uniqueConstraints = @UniqueConstraint(name = "uk_daily_report_user_date", columnNames = {"user_id", "report_date"}), indexes = @Index(name = "idx_daily_report_date", columnList = "report_date"))
public class DailyWorkReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "report_date", nullable = false) private LocalDate reportDate;
    @Lob @Column(nullable = false) private String workSummary = "";
    @Lob private String blockers;
    @Lob private String nextDayPlan;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DailyWorkReportStatus status = DailyWorkReportStatus.DRAFT;
    private LocalDateTime submittedAt;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    protected DailyWorkReport() {}
    public DailyWorkReport(User user, LocalDate date) { this.user = user; this.reportDate = date; this.createdAt = LocalDateTime.now(); this.updatedAt = this.createdAt; }
    public Long getId() { return id; } public User getUser() { return user; } public LocalDate getReportDate() { return reportDate; }
    public String getWorkSummary() { return workSummary; } public String getBlockers() { return blockers; } public String getNextDayPlan() { return nextDayPlan; }
    public DailyWorkReportStatus getStatus() { return status; } public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void update(String summary, String blockers, String plan) { this.workSummary = summary == null ? "" : summary; this.blockers = blockers; this.nextDayPlan = plan; this.updatedAt = LocalDateTime.now(); }
    public void submit() { this.status = DailyWorkReportStatus.SUBMITTED; this.submittedAt = LocalDateTime.now(); this.updatedAt = this.submittedAt; }
}
