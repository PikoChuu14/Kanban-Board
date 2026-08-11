package com.company.kanban.repository;

import com.company.kanban.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardRepository
        extends JpaRepository<Board, Long> {

    List<Board> findByDepartmentId(Long departmentId);

    boolean existsByNameIgnoreCaseAndDepartmentId(
            String name,
            Long departmentId
    );

    Optional<Board> findByNameIgnoreCaseAndDepartmentId(
            String name,
            Long departmentId
    );
}