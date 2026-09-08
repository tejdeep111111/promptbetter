package com.promptbetter.controller;

import com.promptbetter.service.ChallengeService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {
    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping("/domains")
    public ResponseEntity<?> getDomains() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())  //We CACHE PUBLICALLY becasuse that data doesn't change
                .body(challengeService.getDomains());
    }

    @GetMapping
    public ResponseEntity<?> getChallengesByDomain(@RequestParam String domain) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())  //We CACHE PUBLICALLY becasuse that data doesn't change
                .body(challengeService.getChallengesByDomain(domain));
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentChallenge(@RequestParam String domain,@RequestParam int level) {
        try {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())  //We CACHE PUBLICALLY becasuse that data doesn't change
                    .body(challengeService.getChallengeByDomainAndLevel(domain, level));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
