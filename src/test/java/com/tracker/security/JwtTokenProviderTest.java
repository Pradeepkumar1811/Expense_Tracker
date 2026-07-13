package com.tracker.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long-for-hs256";
    private static final long EXPIRATION_MS = 86400000L; // 24 hours

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = tokenProvider.generateToken(1L, "user@example.com");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateToken_shouldContainUserIdAndEmail() {
        Long userId = 42L;
        String email = "test@example.com";

        String token = tokenProvider.generateToken(userId, email);

        assertEquals(userId, tokenProvider.getUserIdFromToken(token));
        assertEquals(email, tokenProvider.getEmailFromToken(token));
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = tokenProvider.generateToken(1L, "user@example.com");

        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForMalformedToken() {
        assertFalse(tokenProvider.validateToken("not.a.valid.token"));
    }

    @Test
    void validateToken_shouldReturnFalseForNullToken() {
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    void validateToken_shouldReturnFalseForEmptyToken() {
        assertFalse(tokenProvider.validateToken(""));
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        // Create a provider with 0ms expiration to generate an already-expired token
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, 0L);
        String token = expiredProvider.generateToken(1L, "user@example.com");

        // Token generated with 0ms expiration should be expired immediately
        assertFalse(tokenProvider.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForTokenSignedWithDifferentSecret() {
        String differentSecret = "different-secret-key-that-is-at-least-32-characters-long";
        SecretKey differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));

        String tokenWithDifferentKey = Jwts.builder()
                .subject("user@example.com")
                .claim("userId", 1L)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(differentKey)
                .compact();

        assertFalse(tokenProvider.validateToken(tokenWithDifferentKey));
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        Long userId = 99L;
        String token = tokenProvider.generateToken(userId, "user@example.com");

        assertEquals(userId, tokenProvider.getUserIdFromToken(token));
    }

    @Test
    void getEmailFromToken_shouldReturnCorrectEmail() {
        String email = "hello@world.com";
        String token = tokenProvider.generateToken(1L, email);

        assertEquals(email, tokenProvider.getEmailFromToken(token));
    }

    @Test
    void getExpirationMs_shouldReturnConfiguredValue() {
        assertEquals(EXPIRATION_MS, tokenProvider.getExpirationMs());
    }

    @Test
    void generateToken_shouldSetFutureExpiration() {
        String token = tokenProvider.generateToken(1L, "user@example.com");

        // Parse the token to check expiration
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date expiration = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        assertTrue(expiration.after(new Date()));
    }

    @Test
    void generateToken_shouldSetIssuedAtToNow() {
        long beforeGeneration = System.currentTimeMillis();
        String token = tokenProvider.generateToken(1L, "user@example.com");
        long afterGeneration = System.currentTimeMillis();

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date issuedAt = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getIssuedAt();

        // issuedAt should be between before and after generation (with some tolerance)
        assertTrue(issuedAt.getTime() >= beforeGeneration - 1000);
        assertTrue(issuedAt.getTime() <= afterGeneration + 1000);
    }
}
