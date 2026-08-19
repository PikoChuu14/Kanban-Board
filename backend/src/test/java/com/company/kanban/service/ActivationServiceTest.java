package com.company.kanban.service;

import com.company.kanban.entity.*; import com.company.kanban.repository.*;
import org.junit.jupiter.api.Test; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime; import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*; import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*;

class ActivationServiceTest {
    @Test void activationIsOneTimeAndActivatesAccount(){ UserRepository users=mock(UserRepository.class);ActivationTokenRepository tokens=mock(ActivationTokenRepository.class);PasswordEncoder encoder=mock(PasswordEncoder.class);
        Department d=new Department("PPC");User user=new User("A","a@test","unused",Role.STAFF,d);user.setStatus(AccountStatus.PENDING_ACTIVATION);ActivationToken token=new ActivationToken(user,"hash",LocalDateTime.now().plusHours(1));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));when(encoder.encode("password1")).thenReturn("encoded");ActivationService service=new ActivationService(users,tokens,encoder,"http://localhost",48);
        service.activate("raw","password1");assertEquals(AccountStatus.ACTIVE,user.getStatus());assertEquals("encoded",user.getPassword());assertNotNull(token.getConsumedAt());assertThrows(ResponseStatusException.class,()->service.activate("raw","password1")); }
    @Test void expiredActivationTokenIsRejected(){UserRepository users=mock(UserRepository.class);ActivationTokenRepository tokens=mock(ActivationTokenRepository.class);PasswordEncoder encoder=mock(PasswordEncoder.class);User user=new User("A","a@test","x",Role.STAFF,new Department("PPC"));ActivationToken token=new ActivationToken(user,"hash",LocalDateTime.now().minusMinutes(1));when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));ActivationService service=new ActivationService(users,tokens,encoder,"http://localhost",48);assertThrows(ResponseStatusException.class,()->service.activate("raw","password1"));}
}
