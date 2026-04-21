package com.promptbetter.controller;

import com.promptbetter.filters.JwtAuthFilter;
import com.promptbetter.model.GlossaryTerm;
import com.promptbetter.repository.GlossaryRepository;
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
 * Tests for GlossaryController.
 * Note: This controller directly uses GlossaryRepository (no service layer),
 * so we mock the repository directly.
 */
@WebMvcTest(GlossaryController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlossaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GlossaryRepository glossaryRepository;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void getAllTerms_shouldReturnMapOfTerms() throws Exception {
        // GIVEN: two glossary terms in the repository
        GlossaryTerm term1 = new GlossaryTerm();
        term1.setTermKey("few_shot");
        term1.setTermDisplay("Few-Shot");
        term1.setDefinition("Providing examples");
        term1.setCategory("technique");

        GlossaryTerm term2 = new GlossaryTerm();
        term2.setTermKey("chain_of_thought");
        term2.setTermDisplay("Chain of Thought");
        term2.setDefinition("Step by step reasoning");
        term2.setCategory(null); // category can be null

        when(glossaryRepository.findAll()).thenReturn(List.of(term1, term2));

        // WHEN + THEN
        mockMvc.perform(get("/api/glossary"))
                .andExpect(status().isOk())
                // The response is a map keyed by termKey
                .andExpect(jsonPath("$.few_shot.display").value("Few-Shot"))
                .andExpect(jsonPath("$.few_shot.definition").value("Providing examples"))
                .andExpect(jsonPath("$.chain_of_thought.display").value("Chain of Thought"))
                // Null category should become empty string
                .andExpect(jsonPath("$.chain_of_thought.category").value(""));
    }

    @Test
    void getAllTerms_empty_shouldReturnEmptyMap() throws Exception {
        when(glossaryRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/glossary"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }
}