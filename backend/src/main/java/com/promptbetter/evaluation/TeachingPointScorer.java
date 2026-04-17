package com.promptbetter.evaluation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class TeachingPointScorer {

    public TeachingPointResult score(TeachingPointRule rule, FactSheet facts) {
        if (rule == null || rule.getMustHave() == null || rule.getMustHave().isEmpty()) {
            return new TeachingPointResult(true, 100, List.of());
        }

        List<String> missed = new ArrayList<>();
        for (TeachingPointRule.Condition condition : rule.getMustHave()) {
            if (!matches(condition, facts)) {
                missed.add(condition.getFact());
            }
        }

        boolean met = missed.isEmpty();
        int passScore = rule.getPassScore() == null ? 100 : clamp(rule.getPassScore(), 0, 100);
        int failScore = rule.getFailScore() == null ? 0 : clamp(rule.getFailScore(), 0, 100);
        return new TeachingPointResult(met, met ? passScore : failScore, missed);
    }

    private boolean matches(TeachingPointRule.Condition condition, FactSheet facts) {
        if (condition == null || condition.getFact() == null || condition.getOperator() == null) {
            return false;
        }

        Object value = readFactValue(condition.getFact(), facts);
        String operator = condition.getOperator().toUpperCase(Locale.ROOT);

        return switch (operator) {
            case "NOT_NULL" -> value instanceof String str && str != null && !str.isBlank();
            case "BOOLEAN_TRUE" -> value instanceof Boolean bool && bool;
            case "BOOLEAN_FALSE" -> value instanceof Boolean bool && !bool;
            case "EQUALS" -> value != null && String.valueOf(value).equalsIgnoreCase(condition.getValue());
            case "CONTAINS_TEXT" -> value instanceof String str
                    && condition.getValue() != null
                    && str != null
                    && str.toLowerCase(Locale.ROOT).contains(condition.getValue().toLowerCase(Locale.ROOT));
            default -> false;
        };
    }

    private Object readFactValue(String factName, FactSheet facts) {
        if (facts == null) {
            return null;
        }

        return switch (factName) {
            case "programming_language" -> facts.getProgrammingLanguage();
            case "ai_role_defined" -> facts.isAiRoleDefined();
            case "output_format_specified" -> facts.isOutputFormatSpecified();
            case "return_type_specified" -> facts.isReturnTypeSpecified();
            case "placeholders_used" -> facts.isPlaceholdersUsed();
            case "length_constraint" -> facts.getLengthConstraint();
            case "audience_defined" -> facts.getAudienceDefined();
            case "step_by_step_requested" -> facts.isStepByStepRequested();
            case "explicit_constraints" -> facts.getExplicitConstraints();
            default -> null;
        };
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record TeachingPointResult(boolean met, int score, List<String> missedFacts) {
    }
}
