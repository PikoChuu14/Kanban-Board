package com.company.kanban.service;

import com.company.kanban.dto.LoginRequest; import com.company.kanban.entity.*; import com.company.kanban.repository.UserRepository;
import org.junit.jupiter.api.Test; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.web.server.ResponseStatusException;
import java.util.Optional; import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;

class AuthServiceStatusTest {
    @Test void disabledUserCannotAuthenticate(){UserRepository users=mock(UserRepository.class);PasswordEncoder encoder=mock(PasswordEncoder.class);JwtService jwt=mock(JwtService.class);User user=new User("Former Staff","off@test","hash",Role.STAFF,new Department("PPC"));user.setStatus(AccountStatus.DISABLED);when(users.findByEmailIgnoreCase("off@test")).thenReturn(Optional.of(user));AuthService service=new AuthService(users,encoder,jwt);assertEquals(401,assertThrows(ResponseStatusException.class,()->service.login(new LoginRequest("off@test","password"))).getStatusCode().value());verify(encoder,never()).matches(anyString(),anyString());}
}
