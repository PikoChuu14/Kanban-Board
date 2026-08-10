package com.company.kanban.config;

import com.company.kanban.entity.Department;
import com.company.kanban.repository.DepartmentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeDepartments(
            DepartmentRepository departmentRepository) {

        return args -> {

            String[] departments = {
                "PPC",
                "PROD",
                "RDD",
                "QC"
            };

            for (String name : departments) {

                if (!departmentRepository.existsByName(name)) {
                    departmentRepository.save(
                        new Department(name)
                    );
                }
            }
        };
    }
}