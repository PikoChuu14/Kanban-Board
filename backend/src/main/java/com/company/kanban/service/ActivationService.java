package com.company.kanban.service;

import com.company.kanban.dto.ActivationLinkResponse;
import com.company.kanban.entity.*;
import com.company.kanban.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class ActivationService {
    private final UserRepository users; private final ActivationTokenRepository tokens; private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom(); private final String baseUrl; private final long expirationHours;
    public ActivationService(UserRepository users, ActivationTokenRepository tokens, PasswordEncoder encoder,
            @Value("${app.base-url:}") String baseUrl,
            @Value("${app.activation.expiration-hours:48}") long expirationHours) {
        this.users=users; this.tokens=tokens; this.encoder=encoder;
        this.baseUrl=baseUrl == null || baseUrl.isBlank() ? "http://localhost:8080" : baseUrl.trim();
        this.expirationHours=expirationHours;
    }
    @Transactional
    public ActivationLinkResponse createLink(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != AccountStatus.PENDING_ACTIVATION) throw new ResponseStatusException(HttpStatus.CONFLICT, "Activation links are only available for pending accounts");
        tokens.deleteByUser(user);
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); LocalDateTime expires = LocalDateTime.now().plusHours(expirationHours);
        tokens.save(new ActivationToken(user, hash(raw), expires));
        return new ActivationLinkResponse(baseUrl.replaceAll("/$", "") + "/activate?token=" + raw, expires);
    }
    @Transactional
    public void activate(String rawToken, String password) {
        ActivationToken token = tokens.findByTokenHash(hash(rawToken)).orElseThrow(() -> invalid());
        if (token.getConsumedAt() != null || !token.getExpiresAt().isAfter(LocalDateTime.now())) throw invalid();
        User user = token.getUser();
        if (user.getStatus() != AccountStatus.PENDING_ACTIVATION) throw invalid();
        user.setPassword(encoder.encode(password)); user.setStatus(AccountStatus.ACTIVE); token.consume(); users.save(user); tokens.save(token);
    }
    private ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation link is invalid, expired, or already used"); }
    private String hash(String raw) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
}
