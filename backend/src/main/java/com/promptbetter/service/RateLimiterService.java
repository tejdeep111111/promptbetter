package com.promptbetter.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {  // Simple in-memory rate limiter
    private final Map<Long, Long> lastSubmissionTime = new ConcurrentHashMap<>();
    private static final long SUBMISSION_INTERVAL_MS = 10;

    public boolean isAllowed(Long userId) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastSubmissionTime.get(userId);

        if (lastTime == null || (currentTime - lastTime) >= SUBMISSION_INTERVAL_MS) {
            lastSubmissionTime.put(userId, currentTime);
            return true;
        }
        return false;
    }

    public long getRemaingTime(Long userId) {
        Long lastTime = lastSubmissionTime.get(userId);
        if (lastTime == null) return 0;

        long elapsed = System.currentTimeMillis() - lastTime;
        return Math.max(0, SUBMISSION_INTERVAL_MS - elapsed);
    }
}
