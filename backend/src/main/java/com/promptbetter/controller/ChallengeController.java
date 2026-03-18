package com.promptbetter.controller;

import com.promptbetter.service.ChallengeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {
    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping("/domains")
    public ResponseEntity<?> getDomains() {
        return ResponseEntity.ok(challengeService.getDomains());
    }

    @GetMapping
    public ResponseEntity<?> getChallengesByDomain(@RequestParam String domain) {
        return ResponseEntity.ok(challengeService.getChallengesByDomain(domain));
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentChallenge(@RequestParam String domain,@RequestParam int level) {
        try {
            return ResponseEntity.ok(challengeService.getChallengeByDomainAndLevel(domain, level));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
