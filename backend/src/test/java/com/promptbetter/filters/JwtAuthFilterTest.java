//package com.promptbetter.filters;
//
//import com.promptbetter.util.JwtUtil;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetailsService;
//
//@ExtendWith(MockitoExtension.class)
//public class JwtAuthFilterTest {
//
//    @Mock
//    private JwtUtil jwtUtil;
//    @Mock
//    private UserDetailsService userDetailsService;
//    @Mock
//    private HttpServletRequest request;
//    @Mock
//    private HttpServletResponse response;
//    @Mock
//    private FilterChain filterChain;
//
//    @InjectMocks
//    private JwtAuthFilter jwtAuthFilter;
//
//    @BeforeEach
//    void setUp() {
//        SecurityContextHolder.clearContext();
//    }
//
//}
