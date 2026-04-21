package com.promptbetter.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubmissonTest {

    @Test
    void createdAt_shouldBeSetAutomaticaly() {
        Submission submission = new Submission();
        assertNotNull(submission.getCreatedAt(), "createdAt should be automatically set when a Submission is created");
    }

    @Test
    void gettersAndSetters_shouldWork() {
        // Create a Submission and set all fields, then verify them.
        // This confirms Lombok's @Data annotation generated everything correctly.
        Submission submission = new Submission();
        submission.setId(1L);
        submission.setUserId(2L);
        submission.setChallengeId(3L);
        submission.setUserPrompt("Test prompt");
        submission.setScore(66);
        submission.setFeedback("Good job!");

        assertEquals(1L, submission.getId());
        assertEquals(2L, submission.getUserId());
        assertEquals(3L, submission.getChallengeId());
        assertEquals("Test prompt", submission.getUserPrompt());
        assertEquals("Test response", submission.getScore());
        assertEquals("Good job!", submission.getFeedback());
    }
}
