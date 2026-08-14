package com.company.kanban.repository;

import com.company.kanban.entity.TaskSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskSnapshotRepository extends JpaRepository<TaskSnapshot, Long> {
    List<TaskSnapshot> findByBatchIdOrderByPositionAsc(Long batchId);
    List<TaskSnapshot> findByBatchIdAndAssigneeIdOrderByStatusAscPositionAsc(Long batchId, Long assigneeId);
}
