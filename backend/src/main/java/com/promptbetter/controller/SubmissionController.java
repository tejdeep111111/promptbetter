package com.promptbetter.controller;

import com.promptbetter.model.User;
import com.promptbetter.service.RateLimiterService;
import com.promptbetter.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;
    private final RateLimiterService rateLimiterService;

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            // auth.getPrincipal() is the User object set by JwtAuthFilter.
            // Cast to User (which implements UserDetails) to get the real ID.
            User currentUser = (User) auth.getPrincipal();
            Long userId = currentUser.getId();

            if(!rateLimiterService.isAllowed(userId)) {
               long remaining = rateLimiterService.getRemaingTime(userId);
               return ResponseEntity.status(429).body(Map.of("error", "Too many submissions. Please wait " + (remaining / 1000) + " seconds."));
            }

            Long challengeId = Long.valueOf(body.get("challengeId").toString());
            String userPrompt = body.get("userPrompt").toString();
            return ResponseEntity.ok(submissionService.submitPrompt(userId, challengeId, userPrompt));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestSubmission(@RequestParam Long challengeId, Authentication auth) {
        try {
            User currentUser = (User) auth.getPrincipal();
            Long userId = currentUser.getId();
            return ResponseEntity.ok(submissionService.getLatestSubmission(userId, challengeId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
