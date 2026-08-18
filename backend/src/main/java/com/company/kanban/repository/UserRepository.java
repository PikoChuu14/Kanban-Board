package com.company.kanban.repository;

import com.company.kanban.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import com.company.kanban.entity.Role;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByDepartmentIdOrderByNameAsc(Long departmentId);

    List<User> findByDepartmentIdAndRole(Long departmentId, Role role);
}
