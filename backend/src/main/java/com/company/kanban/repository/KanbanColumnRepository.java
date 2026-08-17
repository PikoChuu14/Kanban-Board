package com.company.kanban.repository;

import com.company.kanban.entity.KanbanColumn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KanbanColumnRepository
        extends JpaRepository<KanbanColumn, Long> {

    List<KanbanColumn> findByBoardIdOrderByPositionAsc(Long boardId);

    Optional<KanbanColumn> findByBoardIdAndName(Long boardId, String name);

    void deleteByBoardId(Long boardId);
}
