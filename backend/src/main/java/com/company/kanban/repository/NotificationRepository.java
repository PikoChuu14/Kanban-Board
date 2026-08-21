package com.company.kanban.repository;

import com.company.kanban.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdAndClearedAtIsNullOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    long countByRecipientIdAndReadFalseAndClearedAtIsNull(Long recipientId);
    Optional<Notification> findByIdAndRecipientIdAndClearedAtIsNull(Long id, Long recipientId);
    List<Notification> findByRecipientIdAndReadFalseAndClearedAtIsNull(Long recipientId);
    List<Notification> findByRecipientIdAndClearedAtIsNull(Long recipientId);
    void deleteByRecipientId(Long recipientId);
}
