package com.company.kanban.service;

import com.company.kanban.dto.*;
import com.company.kanban.entity.*;
import com.company.kanban.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.lang.reflect.Field; import java.util.*;
import static org.junit.jupiter.api.Assertions.*; import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*;

class AdminUserServiceTest {
    UserRepository users=mock(UserRepository.class); DepartmentRepository departments=mock(DepartmentRepository.class); PasswordEncoder encoder=mock(PasswordEncoder.class);
    ActivationTokenRepository activationTokens=mock(ActivationTokenRepository.class); TaskRepository tasks=mock(TaskRepository.class); DailyWorkReportRepository reports=mock(DailyWorkReportRepository.class); NotificationRepository notifications=mock(NotificationRepository.class);
    UserService service=new UserService(users,departments,encoder,activationTokens,tasks,reports,notifications); Department department;
    @BeforeEach void setup() throws Exception { department=new Department("PPC"); id(department,1L); when(departments.findById(1L)).thenReturn(Optional.of(department)); when(encoder.encode(anyString())).thenReturn("hash"); when(users.save(any())).thenAnswer(i->i.getArgument(0)); }
    @Test void adminCreatesPendingStaffWithoutChoosingPassword(){ when(users.existsByEmailIgnoreCase("new@example.test")).thenReturn(false); UserResponse result=service.createUser(new CreateUserRequest("New Staff","new@example.test",Role.STAFF,1L)); assertEquals(AccountStatus.PENDING_ACTIVATION,result.status()); assertEquals(Role.STAFF,result.role()); }
    @Test void duplicateEmailIsRejected(){ when(users.existsByEmailIgnoreCase("used@example.test")).thenReturn(true); assertThrows(ResponseStatusException.class,()->service.createUser(new CreateUserRequest("Duplicate","used@example.test",Role.MANAGER,1L))); }
    @Test void lastActiveAdminCannotBeDisabledOrDemoted() throws Exception { User admin=new User("Admin","admin@example.test","hash",Role.ADMIN,department); id(admin,7L); when(users.findById(7L)).thenReturn(Optional.of(admin)); when(users.findByRoleAndStatus(Role.ADMIN,AccountStatus.ACTIVE)).thenReturn(List.of(admin));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.disable(7L)).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.updateUser(7L,new UpdateUserRequest("Admin",1L,Role.MANAGER,AccountStatus.ACTIVE))).getStatusCode().value()); }
    @Test void disabledUserWithoutHistoryCanBeDeleted() throws Exception { User user=new User("Former Staff","former@example.test","hash",Role.STAFF,department); id(user,9L); user.setStatus(AccountStatus.DISABLED); when(users.findById(9L)).thenReturn(Optional.of(user));
        service.deleteDisabledUser(9L);
        verify(activationTokens).deleteByUser(user); verify(notifications).deleteByRecipientId(9L); verify(users).delete(user); }
    @Test void activeUserCannotBeDeleted() throws Exception { User user=new User("Active Staff","active@example.test","hash",Role.STAFF,department); id(user,10L); when(users.findById(10L)).thenReturn(Optional.of(user));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.deleteDisabledUser(10L)).getStatusCode().value()); verify(users,never()).delete(any()); }
    @Test void userWithOperationalHistoryCannotBeDeleted() throws Exception { User user=new User("Former Staff","history@example.test","hash",Role.STAFF,department); id(user,11L); user.setStatus(AccountStatus.DISABLED); when(users.findById(11L)).thenReturn(Optional.of(user)); when(tasks.existsByAssigneeIdOrCreatedById(11L,11L)).thenReturn(true);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.deleteDisabledUser(11L)).getStatusCode().value()); verify(users,never()).delete(any()); }
    static void id(Object entity,Long id)throws Exception{Field f=entity.getClass().getDeclaredField("id");f.setAccessible(true);f.set(entity,id);}
}
