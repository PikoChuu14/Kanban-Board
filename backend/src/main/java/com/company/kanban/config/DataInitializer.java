package com.company.kanban.config;

import com.company.kanban.entity.Board;
import com.company.kanban.entity.Department;
import com.company.kanban.entity.KanbanColumn;
import com.company.kanban.repository.BoardRepository;
import com.company.kanban.repository.DepartmentRepository;
import com.company.kanban.repository.KanbanColumnRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@Profile("!demo")
public class DataInitializer {

    private static final String PPC_DEPARTMENT_NAME = "PPC";
    private static final String DEFAULT_BOARD_NAME = "PPC Workflow Board";
    private static final String DEFAULT_BOARD_DESCRIPTION =
        "Workflow board for the PPC team";
    private static final List<String> DEFAULT_COLUMNS = List.of(
        "To Do",
        "In Progress",
        "Review",
        "Done"
    );

    @Bean
    CommandLineRunner initializeData(
            DepartmentRepository departmentRepository,
            BoardRepository boardRepository,
            KanbanColumnRepository kanbanColumnRepository) {

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

            Department ppcDepartment = departmentRepository
                    .findByNameIgnoreCase(PPC_DEPARTMENT_NAME)
                    .orElseThrow(() -> new IllegalStateException(
                            "PPC department is missing"
                    ));

            Board board = boardRepository
                    .findByNameIgnoreCaseAndDepartmentId(
                            DEFAULT_BOARD_NAME,
                            ppcDepartment.getId()
                    )
                    .orElseGet(() -> boardRepository.save(
                            new Board(
                                    DEFAULT_BOARD_NAME,
                                    DEFAULT_BOARD_DESCRIPTION,
                                    ppcDepartment
                            )
                    ));

            Set<String> existingColumns = kanbanColumnRepository
                    .findByBoardIdOrderByPositionAsc(board.getId())
                    .stream()
                    .map(column -> column.getName().toLowerCase())
                    .collect(Collectors.toSet());

            for (int index = 0; index < DEFAULT_COLUMNS.size(); index++) {
                String columnName = DEFAULT_COLUMNS.get(index);

                if (!existingColumns.contains(columnName.toLowerCase())) {
                    kanbanColumnRepository.save(
                            new KanbanColumn(
                                    columnName,
                                    index + 1,
                                    board
                            )
                    );
                }
                    }
        };
    }
}
