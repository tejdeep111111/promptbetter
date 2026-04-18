package com.promptbetter.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeachingPointScorerTest {

    private final TeachingPointScorer scorer = new TeachingPointScorer();

    @Test
    void shouldPassWhenRequiredFactIsPresent() {
        FactSheet facts = new FactSheet();
        facts.setProgrammingLanguage("Python");

        TeachingPointRule.Condition condition = new TeachingPointRule.Condition();
        condition.setFact("programming_language");
        condition.setOperator("NOT_NULL");

        TeachingPointRule rule = new TeachingPointRule();
        rule.setMustHave(List.of(condition));

        TeachingPointScorer.TeachingPointResult result = scorer.score(rule, facts);

        assertTrue(result.met());
        assertEquals(100, result.score());
        assertTrue(result.missedFacts().isEmpty());
    }

    @Test
    void shouldFailWhenRequiredBooleanFactIsMissing() {
        FactSheet facts = new FactSheet();
        facts.setOutputFormatSpecified(false);

        TeachingPointRule.Condition condition = new TeachingPointRule.Condition();
        condition.setFact("output_format_specified");
        condition.setOperator("BOOLEAN_TRUE");

        TeachingPointRule rule = new TeachingPointRule();
        rule.setMustHave(List.of(condition));
        rule.setFailScore(5);

        TeachingPointScorer.TeachingPointResult result = scorer.score(rule, facts);

        assertFalse(result.met());
        assertEquals(5, result.score());
        assertEquals(List.of("output_format_specified"), result.missedFacts());
    }
}
