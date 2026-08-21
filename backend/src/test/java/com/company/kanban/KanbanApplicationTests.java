package com.company.kanban;

import com.company.kanban.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class KanbanApplicationTests {

	@Autowired
	private DepartmentRepository departments;

	@Test
	void contextLoads() {
	}

	@Test
	void maintenanceDepartmentIsCreatedOnStartup() {
		assertTrue(departments.findByNameIgnoreCase("Maintenance").isPresent());
	}

}
