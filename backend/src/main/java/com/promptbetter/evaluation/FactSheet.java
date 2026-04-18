package com.promptbetter.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FactSheet {
    @JsonProperty("programming_language")
    private String programmingLanguage;

    @JsonProperty("ai_role_defined")
    private boolean aiRoleDefined;

    @JsonProperty("output_format_specified")
    private boolean outputFormatSpecified;

    @JsonProperty("return_type_specified")
    private boolean returnTypeSpecified;

    @JsonProperty("placeholders_used")
    private boolean placeholdersUsed;

    @JsonProperty("length_constraint")
    private String lengthConstraint;

    @JsonProperty("audience_defined")
    private String audienceDefined;

    @JsonProperty("step_by_step_requested")
    private boolean stepByStepRequested;

    @JsonProperty("explicit_constraints")
    private String explicitConstraints;
}
