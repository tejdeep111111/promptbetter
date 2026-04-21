package com.promptbetter.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptbetter.model.Challenge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PromptEvaluationOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(PromptEvaluationOrchestrator.class);

    private final PromptFactExtractorService factExtractorService;
    private final TeachingPointScorer teachingPointScorer;
    private final PromptCoachService promptCoachService;
    private final ObjectMapper objectMapper;

    @Value("${app.evaluation.teaching-point-weight:0.8}")
    private double teachingPointWeight;

    @Value("${app.evaluation.general-weight:0.2}")
    private double generalWeight;

    @Value("${app.evaluation.hard-cap-when-missed:60}")
    private int defaultHardCapWhenMissed;

    public PromptEvaluationOrchestrator(PromptFactExtractorService factExtractorService,
                                        TeachingPointScorer teachingPointScorer,
                                        PromptCoachService promptCoachService,
                                        ObjectMapper objectMapper) {
        this.factExtractorService = factExtractorService;
        this.teachingPointScorer = teachingPointScorer;
        this.promptCoachService = promptCoachService;
        this.objectMapper = objectMapper;
    }

    public PromptEvaluationResponse evaluate(Challenge challenge, String userPrompt) {
        FactSheet factSheet = factExtractorService.extractFacts(userPrompt);
        TeachingPointRule rule = parseRule(challenge);
        if (rule == null) {
            rule = new TeachingPointRule();
            rule.setTeachingPoint(challenge.getAiEvaluationGuide());
            rule.setMustHave(List.of());
        }

        TeachingPointScorer.TeachingPointResult teachingResult = teachingPointScorer.score(rule, factSheet);
        PromptCoachFeedback coachFeedback = promptCoachService.generateFeedback(
                challenge.getTask(), userPrompt, factSheet, rule, teachingResult);

        int generalScore = computeGeneralScore(coachFeedback.getDimensions());
        int finalScore = computeFinalScore(
                teachingResult.score(),
                generalScore,
                teachingResult.met(),
                rule,
                teachingPointWeight,
                generalWeight,
                defaultHardCapWhenMissed
        );

        return PromptEvaluationResponse.builder()
                .score(finalScore)
                .finalScore(finalScore)
                .teachingPoint(rule.getTeachingPoint())
                .teachingPointScore(clamp(teachingResult.score(), 0, 100))
                .teachingPointMet(teachingResult.met())
                .generalScore(generalScore)
                .strengths(coachFeedback.getStrengths())
                .flaws(coachFeedback.getFlaws())
                .improvedPrompt(finalScore > 80 ? null : coachFeedback.getImprovedPrompt())
                .explanation(coachFeedback.getExplanation())
                .dimensions(coachFeedback.getDimensions())
                .factSheet(factSheet)
                .build();
    }

    private TeachingPointRule parseRule(Challenge challenge) {
        try {
            String raw = challenge.getTeachingPointRuleJson();
            if (raw == null || raw.isBlank()) {
                TeachingPointRule fallback = new TeachingPointRule();
                fallback.setTeachingPoint(challenge.getAiEvaluationGuide());
                fallback.setMustHave(List.of());
                return fallback;
            }
            return objectMapper.readValue(raw, TeachingPointRule.class);
        } catch (Exception e) {
            log.warn("Invalid teaching_point_rule_json for challenge id={}, using fallback teaching point", challenge.getId(), e);
            TeachingPointRule fallback = new TeachingPointRule();
            fallback.setTeachingPoint(challenge.getAiEvaluationGuide());
            fallback.setMustHave(List.of());
            return fallback;
        }
    }

    public static int computeGeneralScore(Map<String, Integer> dimensions) {
        if (dimensions == null) {
            return 0;
        }
        int clarity = clamp(dimensions.getOrDefault("clarity", 0), 0, 33);
        int specificity = clamp(dimensions.getOrDefault("specificity", 0), 0, 33);
        int context = clamp(dimensions.getOrDefault("context", 0), 0, 33);
        return clamp(clarity + specificity + context, 0, 100);
    }

    public static int computeFinalScore(int teachingPointScore,
                                        int generalScore,
                                        boolean teachingPointMet,
                                        TeachingPointRule rule,
                                        double teachingPointWeight,
                                        double generalWeight,
                                        int defaultHardCapWhenMissed) {
        int clampedTeaching = clamp(teachingPointScore, 0, 100);
        int clampedGeneral = clamp(generalScore, 0, 100);

        double weighted = (teachingPointWeight * clampedTeaching) + (generalWeight * clampedGeneral);
        int finalScore = clamp((int) Math.round(weighted), 0, 100);

        boolean hardCapEnabled = rule == null
                || rule.getApplyHardCapWhenMissed() == null
                || rule.getApplyHardCapWhenMissed();

        if (!teachingPointMet && hardCapEnabled) {
            int cap = defaultHardCapWhenMissed;
            if (rule != null && rule.getHardCapWhenMissed() != null) {
                cap = clamp(rule.getHardCapWhenMissed(), 0, 100);
            }
            finalScore = Math.min(finalScore, cap);
        }

        return clamp(finalScore, 0, 100);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
