package com.promptbetter.controller;

import com.promptbetter.service.SubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {
    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getCredentials();
            Long challengeId = Long.valueOf(body.get("challengeId").toString());
            String userPrompt = body.get("userPrompt").toString();

            return ResponseEntity.ok(submissionService.submitPrompt(userId, challengeId, userPrompt));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

    }
}
