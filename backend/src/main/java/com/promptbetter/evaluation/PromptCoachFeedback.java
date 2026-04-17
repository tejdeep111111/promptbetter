package com.promptbetter.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromptCoachFeedback {
    private List<String> strengths = List.of();
    private List<String> flaws = List.of();

    @JsonProperty("improved_prompt")
    private String improvedPrompt = "";

    private String explanation = "";

    private Map<String, Integer> dimensions = new HashMap<>();
}
