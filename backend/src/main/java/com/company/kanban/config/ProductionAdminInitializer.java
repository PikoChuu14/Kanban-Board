package com.company.kanban.config;

import com.company.kanban.entity.Department;
import com.company.kanban.entity.Role;
import com.company.kanban.entity.User;
import com.company.kanban.repository.DepartmentRepository;
import com.company.kanban.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/** Creates the first administrator only when the production database has no users. */
@Component
@Profile("prod")
public class ProductionAdminInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ProductionAdminInitializer.class);
    private final UserRepository users; private final DepartmentRepository departments;
    private final PasswordEncoder encoder; private final String name; private final String email; private final String password; private final String secretsFile;
    public ProductionAdminInitializer(UserRepository users, DepartmentRepository departments, PasswordEncoder encoder,
            @Value("${app.bootstrap.admin.name:}") String name, @Value("${app.bootstrap.admin.email:}") String email,
            @Value("${app.bootstrap.admin.password:}") String password, @Value("${app.bootstrap.secrets-file:config/secrets.properties}") String secretsFile) {
        this.users=users; this.departments=departments; this.encoder=encoder; this.name=name; this.email=email; this.password=password; this.secretsFile=secretsFile;
    }
    @Override public void run(String... args) {
        if (users.count() > 0 || name.isBlank() || email.isBlank() || password.isBlank()) return;
        Department department = departments.findByNameIgnoreCase("PPC").orElseThrow();
        users.save(new User(name.trim(), email.trim(), encoder.encode(password), Role.ADMIN, department));
        removeBootstrapCredentials();
        log.info("Initial production ADMIN created for {}. Bootstrap credentials were removed automatically.", email);
    }

    private void removeBootstrapCredentials() {
        try {
            Path path = Path.of(secretsFile);
            if (!Files.exists(path)) return;
            String cleaned = Files.lines(path)
                    .filter(line -> !line.startsWith("app.bootstrap.admin."))
                    .collect(Collectors.joining(System.lineSeparator()));
            Files.writeString(path, cleaned + System.lineSeparator());
        } catch (Exception ex) {
            log.warn("Administrator was created, but bootstrap cleanup could not update {}. The credentials are inactive once a user exists.", secretsFile);
        }
    }
}
