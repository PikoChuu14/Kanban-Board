package com.company.kanban.repository;

import com.company.kanban.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import com.company.kanban.entity.Role;
import com.company.kanban.entity.AccountStatus;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByDepartmentIdOrderByNameAsc(Long departmentId);

    List<User> findByDepartmentIdAndRole(Long departmentId, Role role);

    long countByRole(Role role);

    long countByRoleAndStatus(Role role, AccountStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<User> findByRoleAndStatus(Role role, AccountStatus status);

    List<User> findByStatus(AccountStatus status);
}
