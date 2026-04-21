package com.promptbetter.controller;

import com.promptbetter.filters.JwtAuthFilter;
import com.promptbetter.model.Challenge;
import com.promptbetter.service.ChallengeService;
import com.promptbetter.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for ChallengeController.
 */
@WebMvcTest(ChallengeController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChallengeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChallengeService challengeService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void getDomains_shouldReturn200() throws Exception {
        when(challengeService.getDomains()).thenReturn(List.of("coding", "writing"));

        mockMvc.perform(get("/api/challenges/domains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("coding"))
                .andExpect(jsonPath("$[1]").value("writing"));
    }

    @Test
    void getChallengesByDomain_shouldReturn200() throws Exception {
        Challenge c = new Challenge();
        c.setId(1L);
        c.setDomain("coding");
        c.setTitle("Test Challenge");
        c.setTask("Do something");
        c.setLevel(1);

        when(challengeService.getChallengesByDomain("coding")).thenReturn(List.of(c));

        mockMvc.perform(get("/api/challenges").param("domain", "coding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Challenge"));
    }

    @Test
    void getCurrentChallenge_found_shouldReturn200() throws Exception {
        Challenge c = new Challenge();
        c.setId(1L);
        c.setDomain("coding");
        c.setLevel(1);
        c.setTitle("Level 1");
        c.setTask("Task");

        when(challengeService.getChallengeByDomainAndLevel("coding", 1))
                .thenReturn(List.of(c));

        mockMvc.perform(get("/api/challenges/current")
                        .param("domain", "coding")
                        .param("level", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Level 1"));
    }

    @Test
    void getCurrentChallenge_notFound_shouldReturn400() throws Exception {
        when(challengeService.getChallengeByDomainAndLevel("coding", 99))
                .thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/challenges/current")
                        .param("domain", "coding")
                        .param("level", "99"))
                .andExpect(status().isBadRequest());
    }
}