package com.promptbetter.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;


class UserTest {
    //Helper method to create a User instance for testing
    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setName("testuser");
        user.setEmail("testemail@ex.com");
        user.setPassword("testpassword");
        user.setRole("ROLE_USER");
        return user;
    }

    @Test
    void getUsername_shouldReturnEmail() {
        User user = createUser();
        assertEquals("testemail@ex.com", user.getUsername(), "getUsername should return the email");
    }

    @Test
    void getAuthorities_shouldReturnRoleAsGrantedAuthority() {
        // Spring Security uses getAuthorities() to check permissions.
        // Our User wraps the 'role' field into a GrantedAuthority list.
        User user = createUser();
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertEquals(1, authorities.size(), "getAuthorities should return a collection with one authority");
        assertEquals("ROLE_USER", authorities.iterator().next().getAuthority(), "The authority should be ROLE_USER");
    }

    @Test
    void getAuthorities_adminRole() {
        // Verify it works for admin role too
        User user = createUser();
        user.setRole("ROLE_ADMIN");

        assertEquals("ROLE_ADMIN",
                user.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void accountStatusFlags_shouldReturnTrue() {
        User user = createUser();
        assertTrue(user.isAccountNonExpired(), "Account should be non-expired");
        assertTrue(user.isAccountNonLocked(), "Account should be non-locked");
        assertTrue(user.isCredentialsNonExpired(), "Credentials should be non-expired");
        assertTrue(user.isEnabled(), "Account should be enabled");
    }

    @Test
    void defaultRole_shouldBeUser() {
        User user = new User();
        assertEquals("ROLE_USER", user.getAuthorities().iterator().next().getAuthority(), "Default role should be ROLE_USER");
    }

    @Test
    void createdAt_shouldBeSetAutomatically() {
        // The createdAt field is initialized in the field declaration,
        // so it should never be null on a new User.
        User user = new User();
        assertNotNull(user.getCreatedAt(),
                "createdAt should be set automatically");
    }
}
