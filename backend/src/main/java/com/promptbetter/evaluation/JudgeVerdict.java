package com.promptbetter.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Neutral, source-agnostic result of judging a single prompt against a
 * challenge. Produced either by the LLM judge or the deterministic
 * heuristic evaluator, then turned into a final score by the orchestrator.
 *
 * <p>Dimensions are each on a 0-33 scale and are the ONLY thing that drives
 * the final score, so the bars the user sees always match the number.</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JudgeVerdict {

    /** How clear, unambiguous and well-structured the request is (0-33). */
    private int clarity;

    /** How detailed, constrained and concrete the request is (0-33). */
    private int specificity;

    /** How well role, audience, tone and framing are set for the task (0-33). */
    private int context;

    /** Whether the prompt demonstrates the specific skill this challenge teaches. */
    @JsonProperty("teaching_point_met")
    private boolean teachingPointMet;

    private List<String> strengths = new ArrayList<>();

    private List<String> flaws = new ArrayList<>();

    @JsonProperty("improved_prompt")
    private String improvedPrompt = "";

    private String explanation = "";
}
