package com.company.kanban.repository;

import com.company.kanban.entity.ActivationToken;
import com.company.kanban.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
    Optional<ActivationToken> findByTokenHash(String tokenHash);
    void deleteByUser(User user);
}
