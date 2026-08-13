package com.company.kanban.repository;

import com.company.kanban.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository
        extends JpaRepository<Task, Long> {

    List<Task> findByColumnIdOrderByPositionAsc(Long columnId);

    List<Task> findByColumnIdAndIdNotOrderByPositionAsc(
            Long columnId,
            Long taskId
    );

    List<Task> findByAssigneeIdOrderByStatusAscPositionAsc(Long assigneeId);

    int countByColumnId(Long columnId);

    boolean existsByColumnBoardId(Long boardId);
}