package com.promptbetter.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Challenge model.
 * Very simple — just verifying getters/setters work via Lombok @Data.
 */
class ChallengeTest {

    @Test
    void gettersAndSetters_shouldWork() {
        // Create a Challenge and set all fields, then verify them.
        // This confirms Lombok's @Data annotation generated everything correctly.
        Challenge challenge = new Challenge();
        challenge.setId(1L);
        challenge.setDomain("coding");
        challenge.setLevel(3);
        challenge.setHardness("medium");
        challenge.setTopicTaught("prompt clarity");
        challenge.setTitle("Write a clear prompt");
        challenge.setTask("Ask an AI to sort a list");
        challenge.setAiEvaluationGuide("Check for specificity");
        challenge.setTeachingPointRuleJson("{\"rules\":[]}");
        challenge.setConstraintAsString("max 100 words");
        challenge.setKeyTakeaway("Be specific");

        assertEquals(1L, challenge.getId());
        assertEquals("coding", challenge.getDomain());
        assertEquals(3, challenge.getLevel());
        assertEquals("medium", challenge.getHardness());
        assertEquals("prompt clarity", challenge.getTopicTaught());
        assertEquals("Write a clear prompt", challenge.getTitle());
        assertEquals("Ask an AI to sort a list", challenge.getTask());
        assertEquals("Check for specificity", challenge.getAiEvaluationGuide());
        assertEquals("{\"rules\":[]}", challenge.getTeachingPointRuleJson());
        assertEquals("max 100 words", challenge.getConstraintAsString());
        assertEquals("Be specific", challenge.getKeyTakeaway());
    }
}
