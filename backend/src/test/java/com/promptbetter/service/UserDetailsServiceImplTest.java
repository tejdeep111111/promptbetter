package com.promptbetter.service;

import com.promptbetter.model.User;
import com.promptbetter.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_UserExists_ReturnsUserDetails() {
        User user = new User();
        user.setEmail("alice@test.com");
        user.setPassword("password");

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        UserDetails res = userDetailsService.loadUserByUsername("alice@test.com");

        assertEquals("alice@test.com", res.getUsername());
    }

    @Test
    void loadUserByUsername_notFound_shouldThrow() {
        // GIVEN: no user with this email
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        // WHEN + THEN: should throw UsernameNotFoundException (Spring Security convention)
        assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername("nobody@test.com"));
    }
}
