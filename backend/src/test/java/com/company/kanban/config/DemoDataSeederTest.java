package com.company.kanban.config;

import com.company.kanban.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.mockito.Mockito.*;

class DemoDataSeederTest {
    @Test
    void resetDeletesNotificationsBeforeUsers() {
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        DepartmentRepository departments = mock(DepartmentRepository.class);
        UserRepository users = mock(UserRepository.class);
        BoardRepository boards = mock(BoardRepository.class);
        KanbanColumnRepository columns = mock(KanbanColumnRepository.class);
        TaskRepository tasks = mock(TaskRepository.class);
        SnapshotBatchRepository batches = mock(SnapshotBatchRepository.class);
        TaskSnapshotRepository snapshots = mock(TaskSnapshotRepository.class);
        DailyWorkReportRepository reports = mock(DailyWorkReportRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        DemoDataSeeder seeder = new DemoDataSeeder(true, encoder, departments, users,
                boards, columns, tasks, batches, snapshots, reports, notifications);

        seeder.resetDemoData();

        InOrder order = inOrder(notifications, snapshots, batches, reports, tasks, columns, boards, users, departments);
        order.verify(notifications).deleteAllInBatch();
        order.verify(snapshots).deleteAllInBatch();
        order.verify(batches).deleteAllInBatch();
        order.verify(reports).deleteAllInBatch();
        order.verify(tasks).deleteAllInBatch();
        order.verify(columns).deleteAllInBatch();
        order.verify(boards).deleteAllInBatch();
        order.verify(users).deleteAllInBatch();
        order.verify(departments).deleteAllInBatch();
    }
}
