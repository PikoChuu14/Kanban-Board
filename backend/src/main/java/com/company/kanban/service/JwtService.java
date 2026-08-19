package com.company.kanban.service;

import com.company.kanban.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.secret.encoding:raw}") String secretEncoding,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(decodeSecret(secret, secretEncoding));

        this.expirationMs = expirationMs;
    }

    private static byte[] decodeSecret(String secret, String encoding) {
        byte[] keyBytes;
        if ("base64".equalsIgnoreCase(encoding)) {
            try {
                keyBytes = Base64.getDecoder().decode(secret);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "app.jwt.secret must be valid Base64 and decode to at least 32 bytes for HS256.",
                        exception
                );
            }
        } else if ("raw".equalsIgnoreCase(encoding)) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException(
                    "app.jwt.secret.encoding must be 'base64' or 'raw'."
            );
        }

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "app.jwt.secret must decode to at least 32 bytes for HS256."
            );
        }
        return keyBytes;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiration =
                new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);

            return claims.getExpiration()
                    .after(new Date());

        } catch (Exception exception) {
            return false;
        }
    }
}
