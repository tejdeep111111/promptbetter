package com.promptbetter.controller;

import com.promptbetter.model.User;
import com.promptbetter.model.UserProgress;
import com.promptbetter.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProgressController {

    private final UserProgressRepository userProgressRepository;

    @GetMapping("/progress")
    public ResponseEntity<List<UserProgress>> getUserProgress(@AuthenticationPrincipal User user) {
        List<UserProgress> progress = userProgressRepository.findByUserId(user.getId());
        return ResponseEntity.ok(progress);
    }
}
