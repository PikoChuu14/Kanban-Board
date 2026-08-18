package com.company.kanban.config;

import com.company.kanban.entity.*;
import com.company.kanban.repository.BoardRepository;
import com.company.kanban.repository.DailyWorkReportRepository;
import com.company.kanban.repository.DepartmentRepository;
import com.company.kanban.repository.KanbanColumnRepository;
import com.company.kanban.repository.NotificationRepository;
import com.company.kanban.repository.SnapshotBatchRepository;
import com.company.kanban.repository.TaskRepository;
import com.company.kanban.repository.TaskSnapshotRepository;
import com.company.kanban.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/** Deterministic, repeatable demo data. This class is only loaded under the demo profile. */
@Component
@Profile("demo")
public class KovaxDemoDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(KovaxDemoDataSeeder.class);
    private static final LocalDate DEMO_DATE = LocalDate.of(2026, 8, 17);
    private static final ZoneId ZONE = ZoneId.of("Asia/Kuala_Lumpur");
    private static final String PASSWORD = "P@ssw0rd123";
    private static final List<String> DEPARTMENT_NAMES = List.of("PPC", "PROD", "RDD", "QC");
    private static final List<String> COLUMNS = List.of("To Do", "In Progress", "Review", "Done");

    private final boolean reset;
    private final PasswordEncoder encoder;
    private final DepartmentRepository departments;
    private final UserRepository users;
    private final BoardRepository boards;
    private final KanbanColumnRepository columns;
    private final TaskRepository tasks;
    private final SnapshotBatchRepository batches;
    private final TaskSnapshotRepository snapshots;
    private final DailyWorkReportRepository reports;
    private final NotificationRepository notifications;

    public KovaxDemoDataSeeder(@Value("${app.demo.reset-data:false}") boolean reset,
                               PasswordEncoder encoder, DepartmentRepository departments,
                               UserRepository users, BoardRepository boards,
                               KanbanColumnRepository columns, TaskRepository tasks,
                               SnapshotBatchRepository batches, TaskSnapshotRepository snapshots,
                               DailyWorkReportRepository reports, NotificationRepository notifications) {
        this.reset = reset; this.encoder = encoder; this.departments = departments; this.users = users;
        this.boards = boards; this.columns = columns; this.tasks = tasks; this.batches = batches;
        this.snapshots = snapshots; this.reports = reports; this.notifications = notifications;
    }

    @Override @Transactional public void run(String... args) {
        if (!reset) { log.info("Demo data reset disabled."); return; }
        seed();
    }

    protected void seed() {
        log.warn("DEMO MODE: resetting demo database.");
        resetDemoData();

        Map<String, Department> dept = seedDepartments();
        Map<String, User> people = seedUsers(dept);
        Map<String, List<KanbanColumn>> boardColumns = seedBoards(dept);
        List<Task> current = seedTasks(dept, people, boardColumns);
        seedSnapshots(current);
        seedReports(people);
        log.info("Demo credentials: admin@company.com / {}, ahmad@company.com / {}, hana@company.com / {}, amir@company.com / {}, sarah@company.com / {}", PASSWORD, PASSWORD, PASSWORD, PASSWORD, PASSWORD);
        log.info("Demo counts: departments={}, users={}, boards={}, tasks={}, snapshotBatches={}, taskSnapshots={}, dailyReports={}, reviewTasks={}, overdueActiveTasks={}",
                departments.count(), users.count(), boards.count(), tasks.count(), batches.count(), snapshots.count(), reports.count(),
                tasks.findByStatusOrderBySubmittedForReviewAtAsc(TaskStatus.REVIEW).size(), overdueActiveCount(current));
    }

    void resetDemoData() {
        // Notifications own a required foreign key to users, so they must be
        // removed before the demo accounts are reset.
        notifications.deleteAllInBatch();
        snapshots.deleteAllInBatch(); batches.deleteAllInBatch(); reports.deleteAllInBatch();
        tasks.deleteAllInBatch(); columns.deleteAllInBatch(); boards.deleteAllInBatch();
        users.deleteAllInBatch(); departments.deleteAllInBatch();
    }

    private Map<String, Department> seedDepartments() {
        Map<String, Department> result = new LinkedHashMap<>();
        for (String name : DEPARTMENT_NAMES) result.put(name, departments.save(new Department(name)));
        return result;
    }

    private Map<String, User> seedUsers(Map<String, Department> dept) {
        Map<String, User> result = new LinkedHashMap<>();
        add(result, "General Manager", "admin@company.com", Role.ADMIN, dept.get("PPC"));
        addTeam(result, dept.get("PPC"), "Ahmad", "ahmad", List.of("John", "Farid", "Lina", "Aisyah"));
        addTeam(result, dept.get("PROD"), "Daniel", "daniel", List.of("Mei", "Kumar", "Siti", "Raj"));
        addTeam(result, dept.get("RDD"), "Hana", "hana", List.of("Amir", "Jisoo", "Wei", "Nadia"));
        addTeam(result, dept.get("QC"), "Sarah", "sarah", List.of("Afiq", "Nurul", "Adam", "Chen"));
        return result;
    }

    private void addTeam(Map<String, User> result, Department d, String manager, String managerEmail, List<String> staff) {
        add(result, manager, managerEmail + "@company.com", Role.MANAGER, d);
        for (String name : staff) add(result, name, name.toLowerCase(Locale.ROOT) + "@company.com", Role.STAFF, d);
    }

    private void add(Map<String, User> result, String name, String email, Role role, Department d) {
        result.put(name, users.save(new User(name, email, encoder.encode(PASSWORD), role, d)));
    }

    private Map<String, List<KanbanColumn>> seedBoards(Map<String, Department> dept) {
        Map<String, List<KanbanColumn>> result = new LinkedHashMap<>();
        Map<String, List<String>> names = Map.of(
                "PPC", List.of("Production Planning Q3", "Material Coordination", "Customer Schedule Alignment"),
                "PROD", List.of("Assembly Line Improvement", "Machine Setup & Maintenance", "Output Recovery Plan"),
                "RDD", List.of("New Product Development", "CAD & Drawing Improvement", "Prototype Validation"),
                "QC", List.of("Incoming Inspection", "Final Product Inspection", "Quality Improvement Actions"));
        Map<String, String> descriptions = Map.of(
                "PPC", "Planning, material, and customer schedule coordination.", "PROD", "Production line execution and recovery work.",
                "RDD", "Engineering design, prototype, and validation work.", "QC", "Inspection, NCR, and quality improvement work.");
        for (String d : DEPARTMENT_NAMES) for (String boardName : names.get(d)) {
            Board b = boards.save(new Board(boardName, descriptions.get(d), dept.get(d)));
            List<KanbanColumn> cols = new ArrayList<>();
            for (int i = 0; i < COLUMNS.size(); i++) cols.add(columns.save(new KanbanColumn(COLUMNS.get(i), i + 1, b)));
            result.put(d + ":" + boardName, cols);
        }
        return result;
    }

    private List<Task> seedTasks(Map<String, Department> dept, Map<String, User> people, Map<String, List<KanbanColumn>> boardColumns) {
        Map<String, List<String>> titles = Map.of(
                "PPC", List.of("Update weekly production schedule", "Confirm raw material availability", "Revise customer delivery plan", "Resolve machine capacity conflict", "Update production forecast", "Prepare planning meeting data", "Validate stock projection", "Review outstanding production orders", "Coordinate urgent material shortage", "Prepare monthly output plan", "Adjust production sequence", "Confirm customer delivery priority", "Reconcile supplier delivery dates", "Prepare capacity review", "Update backlog risk list", "Align weekend production plan", "Check open purchase commitments", "Publish revised dispatch plan"),
                "PROD", List.of("Calibrate assembly station", "Prepare machine changeover", "Investigate line stoppage", "Update operator work instruction", "Check tooling condition", "Recover delayed production output", "Validate machine setup parameters", "Replace worn fixture", "Prepare preventive maintenance checklist", "Review daily production output", "Optimize assembly sequence", "Verify safety interlock", "Investigate cycle-time increase", "Confirm spare parts availability", "Balance operator allocation", "Audit line start-up checklist", "Trial revised workholding", "Close production recovery actions"),
                "RDD", List.of("Update prototype CAD model", "Prepare BOM revision", "Verify component dimensions", "Design mounting bracket", "Update assembly drawing", "Review engineering change", "Prototype fit verification", "Modify enclosure design", "Research alternate material", "Prepare design review", "Create prototype test procedure", "Update tolerance specification", "Validate revised component", "Prepare drawing release package", "Update prototype drawing package", "Check supplier component interface", "Document prototype lessons learned", "Prepare validation sample list"),
                "QC", List.of("Inspect incoming material batch", "Prepare NCR investigation", "Review dimensional inspection results", "Verify final product checklist", "Update quality inspection plan", "Perform first article inspection", "Investigate recurring defect", "Prepare corrective action report", "Review supplier quality issue", "Verify measurement equipment", "Perform outgoing inspection", "Update control plan", "Audit inspection records", "Confirm gauge calibration status", "Review containment effectiveness", "Prepare defect trend summary", "Verify packaging inspection", "Close quality improvement action"));
        Map<String, List<String>> boardNames = Map.of("PPC", List.of("Production Planning Q3", "Material Coordination", "Customer Schedule Alignment"), "PROD", List.of("Assembly Line Improvement", "Machine Setup & Maintenance", "Output Recovery Plan"), "RDD", List.of("New Product Development", "CAD & Drawing Improvement", "Prototype Validation"), "QC", List.of("Incoming Inspection", "Final Product Inspection", "Quality Improvement Actions"));
        List<Task> result = new ArrayList<>();
        TaskStatus[] status = {TaskStatus.DRAFT, TaskStatus.DOING, TaskStatus.REVIEW, TaskStatus.DONE, TaskStatus.DOING, TaskStatus.DRAFT, TaskStatus.REVIEW, TaskStatus.DONE, TaskStatus.DOING, TaskStatus.DRAFT, TaskStatus.REVIEW, TaskStatus.DOING, TaskStatus.DRAFT, TaskStatus.REVIEW, TaskStatus.DOING, TaskStatus.DONE, TaskStatus.DRAFT, TaskStatus.DOING};
        Priority[] priority = {Priority.MEDIUM, Priority.HIGH, Priority.MEDIUM, Priority.LOW, Priority.MEDIUM, Priority.HIGH};
        for (String d : DEPARTMENT_NAMES) {
            String manager = Map.of("PPC", "Ahmad", "PROD", "Daniel", "RDD", "Hana", "QC", "Sarah").get(d);
            List<String> staff = switch (d) { case "PPC" -> List.of("John", "Farid", "Lina", "Aisyah"); case "PROD" -> List.of("Mei", "Kumar", "Siti", "Raj"); case "RDD" -> List.of("Amir", "Jisoo", "Wei", "Nadia"); default -> List.of("Afiq", "Nurul", "Adam", "Chen"); };
            List<String> bs = boardNames.get(d);
            for (int i = 0; i < 18; i++) {
                User assignee = people.get(staff.get(i % staff.size()));
                if (d.equals("RDD") && i == 14) assignee = people.get("Amir");
                User creator = i % 3 == 0 ? assignee : people.get(manager);
                Task t = new Task(titles.get(d).get(i), "Demo work item for " + d + " operational review.", priority[i % priority.length], dueDate(i, status[i]), i % 6, boardColumns.get(d + ":" + bs.get(i % 3)).get(columnIndex(status[i])), assignee);
                t.setStatus(status[i]); t.setWorkload(d.equals("RDD") && i == 14 ? 3 : 1 + ((i * 2 + d.length()) % 5)); t.setCreatedBy(creator);
                if (status[i] == TaskStatus.REVIEW) t.setSubmittedForReviewAt(DEMO_DATE.atTime(9 + i % 4, 15 + (i * 5) % 40));
                result.add(tasks.save(t));
            }
        }
        // The named reassignment story is deliberately assigned to high-workload Amir and low-workload Nadia.
        return result;
    }

    private LocalDate dueDate(int i, TaskStatus status) {
        if (status == TaskStatus.DONE) return DEMO_DATE.minusDays(2 + i % 5);
        return switch (i % 9) { case 0 -> DEMO_DATE.minusDays(4); case 1 -> DEMO_DATE.minusDays(3); case 2 -> DEMO_DATE.minusDays(1); case 3 -> DEMO_DATE; case 4 -> DEMO_DATE.plusDays(1); case 5 -> DEMO_DATE.plusDays(2); case 6 -> DEMO_DATE.plusDays(3); case 7 -> DEMO_DATE.plusDays(7); default -> null; };
    }

    private int columnIndex(TaskStatus s) { return switch (s) { case DRAFT -> 0; case DOING -> 1; case REVIEW -> 2; case DONE -> 3; }; }

    private void seedSnapshots(List<Task> current) {
        List<LocalDate> dates = List.of(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11), LocalDate.of(2026, 8, 12), LocalDate.of(2026, 8, 13), LocalDate.of(2026, 8, 14), DEMO_DATE);
        for (LocalDate date : dates) {
            createSnapshot(date, SnapshotType.START_OF_DAY, false, current, date.equals(LocalDate.of(2026, 8, 13)));
            if (!date.equals(DEMO_DATE)) createSnapshot(date, SnapshotType.END_OF_DAY, date.equals(LocalDate.of(2026, 8, 12)), current, date.equals(LocalDate.of(2026, 8, 13)));
        }
    }

    private void createSnapshot(LocalDate date, SnapshotType type, boolean recovered, List<Task> current, boolean movementDay) {
        LocalTime time = type == SnapshotType.START_OF_DAY ? LocalTime.of(8, 0) : LocalTime.of(17, 0);
        SnapshotBatch b = batches.save(new SnapshotBatch(date, type, LocalDateTime.of(date, time), recovered));
        List<TaskSnapshot> copy = new ArrayList<>();
        for (Task t : current) {
            TaskSnapshot s = new TaskSnapshot(b, t);
            if (movementDay && t.getTitle().equals("Create prototype test procedure")) { s.setStatus(type == SnapshotType.START_OF_DAY ? TaskStatus.DOING : TaskStatus.REVIEW); s.setColumnName(type == SnapshotType.START_OF_DAY ? "In Progress" : "Review"); }
            if (movementDay && t.getTitle().equals("Update prototype drawing package") && type == SnapshotType.START_OF_DAY) { s.setStatus(TaskStatus.DOING); s.setColumnName("In Progress"); }
            copy.add(s);
        }
        snapshots.saveAll(copy); b.complete(date.atTime(type == SnapshotType.START_OF_DAY ? 8 : 17, type == SnapshotType.START_OF_DAY ? 0 : 12).atZone(ZONE).toLocalDateTime(), copy.size()); batches.save(b);
    }

    private void seedReports(Map<String, User> people) {
        List<String> names = List.of("John", "Farid", "Lina", "Aisyah", "Mei", "Kumar", "Siti", "Raj", "Amir", "Jisoo", "Wei", "Nadia", "Afiq", "Nurul", "Adam", "Chen", "Hana", "Daniel");
        for (int day = 10; day <= 14; day++) for (int i = 0; i < names.size(); i++) {
            if ((day + i) % 7 == 0 || (day == 14 && names.get(i).equals("Nadia"))) continue; // NOT_STARTED is represented by no row.
            User u = people.get(names.get(i)); DailyWorkReport r = new DailyWorkReport(u, LocalDate.of(2026, 8, day));
            r.update(summary(u), "Waiting for cross-team confirmation on one open item.", "Continue the planned verification and communicate any change in priority.");
            if ((day + i) % 6 != 0 && !(day == 14 && names.get(i).equals("Wei"))) r.submit(); reports.save(r);
        }
        createReport(people.get("Amir"), DEMO_DATE, true); createReport(people.get("Jisoo"), DEMO_DATE, false); createReport(people.get("Nadia"), DEMO_DATE, true);
    }

    private void createReport(User u, LocalDate date, boolean submitted) { DailyWorkReport r = new DailyWorkReport(u, date); r.update(summary(u), "Final machining tolerance is pending confirmation from Production.", "Complete the revision and submit the updated work for review."); if (submitted) r.submit(); reports.save(r); }
    private String summary(User u) { return switch (u.getDepartment().getName()) { case "RDD" -> "Updated the prototype assembly drawing and verified revised dimensions against the latest production feedback."; case "PPC" -> "Updated the weekly production schedule and adjusted delivery priorities based on material availability."; case "PROD" -> "Completed machine changeover setup and verified operating parameters before production restart."; default -> "Completed incoming inspection for the latest supplier batch and recorded dimensional results."; }; }
    private long overdueActiveCount(List<Task> current) { return current.stream().filter(t -> t.getStatus() != TaskStatus.DONE && t.getDueDate() != null && t.getDueDate().isBefore(DEMO_DATE)).count(); }
}
