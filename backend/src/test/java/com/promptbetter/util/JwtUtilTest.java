package com.promptbetter.util;

import com.promptbetter.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;


class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        setField(jwtUtil, "secretKey", "testSecretKeyerdfgsdgsfdghwrdfg");
        setField(jwtUtil, "expirationTime", 3600000L); // 1 hour
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = JwtUtil.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /** Creates a fake User to use in tests */
    private User createTestUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password");
        return user;
    }

    @Test
    void generateToken_shouldReturnNonNullString() {
        // The most basic check: generating a token shouldn't return null
        User user = createTestUser();
        String token = jwtUtil.generateToken(user);
        assertNotNull(token, "Token should not be null");
        assertFalse(token.isEmpty(), "Token should not be empty");
    }

    @Test
    void extractUsername_shouldReturnEmail() {
        // Generate a token, then extract the username (email) from it.
        // They should match.
        User user = createTestUser();
        String token = jwtUtil.generateToken(user);

        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals("test@example.com", extractedUsername,
                "Extracted username should match the email used to create the token");
    }

    @Test
    void isTokenValid_shouldReturnTrue_forValidToken() {
        // A freshly generated token should be valid for the same user
        User user = createTestUser();
        String token = jwtUtil.generateToken(user);

        assertTrue(jwtUtil.isTokenValid(token, user),
                "A fresh token should be valid for the user who created it");
    }

    @Test
    void isTokenValid_shouldReturnFalse_forDifferentUser() {
        // A token generated for one user should NOT be valid for another user
        User user1 = createTestUser();
        User user2 = new User();
        user2.setEmail("other@example.com");
        user2.setPassword("password");

        String token = jwtUtil.generateToken(user1);

        assertFalse(jwtUtil.isTokenValid(token, user2),
                "Token should not be valid for a different user");
    }

    @Test
    void isTokenValid_shouldReturnFalse_forExpiredToken() throws Exception {
        // Set expiration to 0ms (token expires immediately)
        setField(jwtUtil, "jwtExpiration", 0L);

        User user = createTestUser();
        String token = jwtUtil.generateToken(user);

        // Small delay to ensure the token is expired
        Thread.sleep(10);

        assertFalse(jwtUtil.isTokenValid(token, user),
                "An expired token should not be valid");
    }
}