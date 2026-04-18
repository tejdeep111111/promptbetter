package com.promptbetter.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeachingPointRule {
    @JsonProperty("teaching_point")
    private String teachingPoint;

    @JsonProperty("must_have")
    private List<Condition> mustHave = new ArrayList<>();

    @JsonProperty("apply_hard_cap_when_missed")
    private Boolean applyHardCapWhenMissed;

    @JsonProperty("hard_cap_when_missed")
    private Integer hardCapWhenMissed;

    @JsonProperty("pass_score")
    private Integer passScore;

    @JsonProperty("fail_score")
    private Integer failScore;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Condition {
        private String fact;
        private String operator;
        private String value;
    }
}
