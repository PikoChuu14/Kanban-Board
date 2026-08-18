package com.company.kanban.repository;
import com.company.kanban.entity.DailyWorkReport;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DailyWorkReportRepository extends JpaRepository<DailyWorkReport, Long> {
    Optional<DailyWorkReport> findByUserIdAndReportDate(Long userId, LocalDate date);
    List<DailyWorkReport> findByReportDate(LocalDate date);
    List<DailyWorkReport> findByReportDateBetween(LocalDate start, LocalDate end);
}
