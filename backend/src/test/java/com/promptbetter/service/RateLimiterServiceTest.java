package com.promptbetter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

class RateLimiterServiceTest {
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() throws Exception {
        rateLimiterService = new RateLimiterService();

        Field field = RateLimiterService.class.getDeclaredField("SUBMISSION_INTERVAL_MS");
        field.setAccessible(true);
        field.set(rateLimiterService, 1000L); // Set to 1 second for testing
    }

    @Test
    void isAllowed_FirstSubmission_ReturnsTrue() {
        assert rateLimiterService.isAllowed(1L);
    }

    @Test
    void isAllowed_secondSubmissionTooSoon_ReturnsFalse() {
        rateLimiterService.isAllowed(1L); // First submission
        assert !rateLimiterService.isAllowed(1L); // Second submission immediately
    }

    @Test
    void isAllowed_differentUser_ReturnsTrue() {
        rateLimiterService.isAllowed(1L); // User 1 submits
        assert rateLimiterService.isAllowed(2L); // User 2 should be allowed
    }

    @Test
    void isAllowed_submissionAfterInterval_ReturnsTrue() throws InterruptedException {
        rateLimiterService.isAllowed(1L); // First submission
        Thread.sleep(1100); // Wait longer than the interval
        assert rateLimiterService.isAllowed(1L); // Should be allowed again
    }

    @Test
    void getRemainingTime_afterSubmission_ReturnsPositive() {
        rateLimiterService.isAllowed(1L); // First submission
        long remaining = rateLimiterService.getRemainingTime(1L);
        assert remaining > 0 && remaining <= 1000L;
    }

    @Test
    void getRemainingTime_noSubmission_ReturnsZero() {
        long remaining = rateLimiterService.getRemainingTime(1L);
        assert remaining == 0;
    }
}
