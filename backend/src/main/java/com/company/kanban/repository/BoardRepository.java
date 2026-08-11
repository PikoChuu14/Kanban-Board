package com.company.kanban.repository;

import com.company.kanban.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository
        extends JpaRepository<Board, Long> {

    List<Board> findByDepartmentId(Long departmentId);

    boolean existsByNameIgnoreCaseAndDepartmentId(
            String name,
            Long departmentId
    );
}