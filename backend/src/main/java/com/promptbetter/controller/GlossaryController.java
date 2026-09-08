package com.promptbetter.controller;

import com.promptbetter.model.GlossaryTerm;
import com.promptbetter.repository.GlossaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/glossary")
@RequiredArgsConstructor
public class GlossaryController {

    private final GlossaryRepository glossaryRepository;

    /**
     * Get all glossary terms - cached for performance
     * Frontend calls this once at login and caches locally
     */
    @GetMapping

    public ResponseEntity<?> getAllTerms() {
       return ResponseEntity.ok(getGlossary());
    }

    @Cacheable(value = "glossary")
    public Map<String, Map<String, String>> getGlossary() {
        return glossaryRepository.findAll().stream()
            .collect(Collectors.toMap(
                GlossaryTerm::getTermKey,
                term -> Map.of(
                    "display", term.getTermDisplay(),
                    "definition", term.getDefinition(),
                    "category", term.getCategory() != null ? term.getCategory() : ""
                )
            ));
    }
}