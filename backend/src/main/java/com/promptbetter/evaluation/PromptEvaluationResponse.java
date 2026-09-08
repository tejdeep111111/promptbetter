package com.promptbetter.evaluation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Serialized as the {@code feedback} payload persisted per submission and
 * consumed by the frontend. Every field the UI renders lives here.
 */
@Getter
@Builder
public class PromptEvaluationResponse {

    /** Final 0-100 score. Mirrors {@link #finalScore} for the UI. */
    private final int score;

    @JsonProperty("final_score")
    private final int finalScore;

    /** The specific skill this challenge is teaching. */
    @JsonProperty("teaching_point")
    private final String teachingPoint;

    @JsonProperty("teaching_point_met")
    private final boolean teachingPointMet;

    /** Raw sum of the three dimensions (0-99), before scaling/capping. */
    @JsonProperty("general_score")
    private final int generalScore;

    private final List<String> strengths;
    private final List<String> flaws;

    @JsonProperty("improved_prompt")
    private final String improvedPrompt;

    private final String explanation;

    /** clarity / specificity / context, each 0-33. Drives the score bars. */
    private final Map<String, Integer> dimensions;

    /** "ai" when the LLM judge scored it, "heuristic" on fallback. */
    @JsonProperty("evaluated_by")
    private final String evaluatedBy;
}
