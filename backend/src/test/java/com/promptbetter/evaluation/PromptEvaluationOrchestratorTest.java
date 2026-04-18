package com.promptbetter.evaluation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PromptEvaluationOrchestratorTest {

    @Test
    void computeGeneralScoreShouldClampEachDimensionAndTotal() {
        int score = PromptEvaluationOrchestrator.computeGeneralScore(Map.of(
                "clarity", 30,
                "context", -5,
                "specificity", 20,
                "constraints", 15,
                "technique", 10
        ));

        assertEquals(65, score);
    }

    @Test
    void computeFinalScoreShouldApplyHardCapWhenTeachingPointMissed() {
        TeachingPointRule rule = new TeachingPointRule();
        rule.setApplyHardCapWhenMissed(true);
        rule.setHardCapWhenMissed(40);

        int score = PromptEvaluationOrchestrator.computeFinalScore(
                50,
                100,
                false,
                rule,
                0.8,
                0.2,
                60
        );

        assertEquals(40, score);
    }

    @Test
    void computeFinalScoreShouldRespectWeightedMathAndClamping() {
        int score = PromptEvaluationOrchestrator.computeFinalScore(
                100,
                100,
                true,
                null,
                0.8,
                0.2,
                60
        );

        assertEquals(100, score);
    }
}
