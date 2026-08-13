package com.company.kanban.service;

import com.company.kanban.dto.CreateUserRequest;
import com.company.kanban.dto.UserResponse;
import com.company.kanban.entity.Department;
import com.company.kanban.entity.Role;
import com.company.kanban.entity.User;
import com.company.kanban.repository.DepartmentRepository;
import com.company.kanban.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

        @Transactional(readOnly = true)
        public List<UserResponse> getAssignableUsers(User currentUser) {
                List<User> users;

                if (currentUser.getRole() == Role.ADMIN) {
                        users = userRepository.findAll();
                } else {
                        users = userRepository.findByDepartmentIdOrderByNameAsc(
                                        currentUser.getDepartment().getId()
                        );
                }

                return users.stream()
                                .map(this::toResponse)
                                .toList();
        }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        return toResponse(user);
    }

    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email already exists"
            );
        }

        Department department =
                departmentRepository.findById(request.departmentId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Department not found"
                                )
                        );

        String hashedPassword =
                passwordEncoder.encode(request.password());

        User user = new User(
                request.name(),
                request.email(),
                hashedPassword,
                request.role(),
                department
        );

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    private UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartment().getId(),
                user.getDepartment().getName(),
                user.getCreatedAt()
        );
    }
}