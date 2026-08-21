package com.company.kanban.service;

import com.company.kanban.config.CompanyAddress;
import com.company.kanban.dto.ActivationLinkResponse;
import com.company.kanban.entity.*;
import com.company.kanban.repository.ActivationTokenRepository;
import com.company.kanban.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ActivationServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ActivationTokenRepository tokens = mock(ActivationTokenRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    @Test
    void configuredCompanyAddressIsAlwaysUsedForActivationLinks() {
        User user = pendingUser();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        ActivationService service = service("http://flowops-server:8080/", false);

        ActivationLinkResponse response = service.createLink(7L);

        assertTrue(response.activationLink().startsWith("http://flowops-server:8080/activate?token="));
        assertFalse(response.activationLink().contains("localhost"));
        verify(tokens).deleteByUser(user);
        verify(tokens).save(any(ActivationToken.class));
    }

    @Test
    void missingCompanyAddressRejectsLinkBeforeChangingTokens() {
        ActivationService service = service("", false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.createLink(7L));

        assertEquals(503, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Configure APP_BASE_URL"));
        verifyNoInteractions(users, tokens);
    }

    @Test
    void localhostIsRejectedForProductionActivationLinks() {
        ActivationService service = service("http://localhost:8080", false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.createLink(7L));

        assertEquals(503, exception.getStatusCode().value());
        verifyNoInteractions(users, tokens);
    }

    @Test
    void localhostCanBeExplicitlyEnabledForDevelopment() {
        User user = pendingUser();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        ActivationService service = service("http://localhost:5173", true);

        assertTrue(service.createLink(7L).activationLink().startsWith("http://localhost:5173/activate?token="));
    }

    @Test
    void activationIsOneTimeAndActivatesAccount() {
        User user = pendingUser();
        ActivationToken token = new ActivationToken(user, "hash", LocalDateTime.now().plusHours(1));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(encoder.encode("password1")).thenReturn("encoded");
        ActivationService service = service("http://flowops-server:8080", false);

        service.activate("raw", "password1");

        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        assertEquals("encoded", user.getPassword());
        assertNotNull(token.getConsumedAt());
        assertThrows(ResponseStatusException.class, () -> service.activate("raw", "password1"));
    }

    @Test
    void expiredActivationTokenIsRejected() {
        User user = pendingUser();
        ActivationToken token = new ActivationToken(user, "hash", LocalDateTime.now().minusMinutes(1));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        ActivationService service = service("http://flowops-server:8080", false);

        assertThrows(ResponseStatusException.class, () -> service.activate("raw", "password1"));
    }

    private ActivationService service(String baseUrl, boolean allowLocalhost) {
        return new ActivationService(users, tokens, encoder, new CompanyAddress(baseUrl, allowLocalhost), 48);
    }

    private User pendingUser() {
        User user = new User("A", "a@test", "unused", Role.STAFF, new Department("PPC"));
        user.setStatus(AccountStatus.PENDING_ACTIVATION);
        return user;
    }
}
