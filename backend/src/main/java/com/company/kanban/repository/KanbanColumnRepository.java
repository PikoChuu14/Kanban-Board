package com.company.kanban.repository;

import com.company.kanban.entity.KanbanColumn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KanbanColumnRepository
        extends JpaRepository<KanbanColumn, Long> {

    List<KanbanColumn> findByBoardIdOrderByPositionAsc(Long boardId);
}