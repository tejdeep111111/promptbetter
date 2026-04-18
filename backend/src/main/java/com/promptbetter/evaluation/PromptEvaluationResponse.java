package com.promptbetter.evaluation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class PromptEvaluationResponse {
    private final int score;

    @JsonProperty("final_score")
    private final int finalScore;

    @JsonProperty("teaching_point")
    private final String teachingPoint;

    @JsonProperty("teaching_point_score")
    private final int teachingPointScore;

    @JsonProperty("teaching_point_met")
    private final boolean teachingPointMet;

    @JsonProperty("general_score")
    private final int generalScore;

    private final List<String> strengths;
    private final List<String> flaws;

    @JsonProperty("improved_prompt")
    private final String improvedPrompt;

    private final String explanation;
    private final Map<String, Integer> dimensions;

    @JsonProperty("fact_sheet")
    private final FactSheet factSheet;
}
