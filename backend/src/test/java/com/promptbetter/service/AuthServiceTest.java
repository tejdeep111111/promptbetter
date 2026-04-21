package com.promptbetter.service;

import com.promptbetter.model.User;
import com.promptbetter.repository.UserRepository;
import com.promptbetter.util.EmailValidator;
import com.promptbetter.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * Tests for AuthService.
 * KEY CONCEPTS FOR BEGINNERS:
 * - @Mock creates a fake version of a dependency (e.g., UserRepository).
 *   The fake does nothing by default — we tell it what to return using when(...).thenReturn(...).
 * - @InjectMocks creates the real AuthService, but injects the mocks into it.
 * - This way we test AuthService logic WITHOUT needing a real database or JWT library.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository; // Fake DB
    @Mock
    private PasswordEncoder passwordEncoder; // Fake password hasher
    @Mock
    private JwtUtil jwtUtil; // Fake JWT generator
    @Mock
    private EmailValidator emailValidator; // Fake email validator

    @InjectMocks
    private AuthService authService; // The real service we want to test

    //Tests start FROM HERE. We will write tests for register and login methods, using the mocks to control the behavior of dependencies.

    //REGISTER TESTS
    @Test
    void register_success() {
        // Email validator returns null = valid
        when(emailValidator.validate("alice@test.com")).thenReturn(null);
        //Tells the fake UserRepository: "If someone asks whether alice@test.com exists, say no."
        //This simulates a brand-new user who hasn't registered before.
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        //Tells the fake PasswordEncoder: "When asked to hash "password", return "hashedPassword"."
        when(passwordEncoder.encode("password")).thenReturn("hashedPassword");
        //any(User.class) means: "match any User object passed to save()."
        //.thenAnswer(...) is used instead of .thenReturn(...) because we need to intercept the actual object being saved and mutate it.
        //invocation.getArgument(0) grabs the first argument passed to save() — the User object.
        //saved.setId(1L) simulates what a real database would do: assign an auto-generated ID.
        when(userRepository.save(any(User.class))).thenAnswer(invocationOnMock -> {
            User saved = invocationOnMock.getArgument(0);
            saved.setId(1L); // Simulate DB assigning an ID
            return saved;
        });
        //Tells the fake JwtUtil: "For any User, return "fake-jwt-token"
        when(jwtUtil.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        //This calls the actual AuthService.register() method — not a mock.
        //Internally, register() will call userRepository.existsByEmail(), passwordEncoder.encode(), userRepository.save(), and jwtUtil.generateToken() — all of which are intercepted by our mocks.
        Map<String, Object> response = authService.register("Alice", "alice@test.com", "password");
        assertEquals("fake-jwt-token", response.get("token"));
        assertEquals(1L, response.get("id"));
        assertEquals("Alice", response.get("name"));
        assertEquals("alice@test.com", response.get("email"));
        //Confirms that userRepository.save() was called exactly once.
        //Even if assertions pass, this catches bugs like calling save() twice or not at all.
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_shouldThrow() {
        //Email passes validation
        when(emailValidator.validate("alice@test.com")).thenReturn(null);
        //Simulate that the email already exists in the database
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                authService.register("Alice", "alice@test.com", "password"));

        assertEquals("Email already in use", ex.getMessage());

        //Verify that save() was never called, since registration should fail before that step
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_blankName_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.register("", "alice@test.com", "password"));
    }

    @Test
    void register_nullEmail_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.register("Alice", null, "password"));
    }

    @Test
    void register_nullPassword_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.register("Alice", "alice@test.com", null));
    }

    @Test
    void register_invalidEmail_shouldThrow() {
        // EmailValidator returns an error message for a bad email
        when(emailValidator.validate("not-an-email")).thenReturn("Invalid email format");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.register("Alice", "not-an-email", "password"));
        assertEquals("Invalid email format", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_fakeDomain_shouldThrow() {
        when(emailValidator.validate("user@fakedomain12345.xyz")).thenReturn("Email domain \"fakedomain12345.xyz\" does not appear to accept mail");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                authService.register("Alice", "user@fakedomain12345.xyz", "password"));
        assertTrue(ex.getMessage().contains("does not appear to accept mail"));
        verify(userRepository, never()).save(any(User.class));
    }

    //LOGIN TESTS
    @Test
    void login_success() {
        User user = new User();
        user.setId(1L);
        user.setName("Alice");
        user.setEmail("alice@test.com");
        user.setPassword("hashedPassword");

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("fake-jwt-token");

        Map<String, Object> result = authService.login("alice@test.com", "password");
        assertEquals("fake-jwt-token", result.get("token"));
        assertEquals("Alice", result.get("name"));
    }

    @Test
    void login_userNotFound_shouldThrow() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                authService.login("nobody@test.com", "password"));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void login_wrongPassword_shouldThrow() {
        User user = new User();
        user.setEmail("alice@test.com");
        user.setPassword("hashedPassword");

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                authService.login("alice@test.com", "wrongpassword"));
        assertEquals("Invalid password", ex.getMessage());
    }
}