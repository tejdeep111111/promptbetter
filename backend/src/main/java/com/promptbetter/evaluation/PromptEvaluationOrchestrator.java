package com.promptbetter.evaluation;

import com.promptbetter.model.Challenge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a user's prompt into a meaningful, self-consistent evaluation.
 *
 * <p>Pipeline: the challenge-aware {@link PromptJudgeService} grades the prompt;
 * if that is unavailable the deterministic {@link HeuristicPromptEvaluator}
 * takes over. Either way the final 0-100 score is derived DIRECTLY from the
 * three dimensions, so the bars the user sees always match the number. When the
 * skill the challenge teaches is not demonstrated, the score is capped.</p>
 */
@Service
public class PromptEvaluationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PromptEvaluationOrchestrator.class);

    /** Max dimension total (clarity + specificity + context). */
    private static final int MAX_DIMENSION_TOTAL = 99;

    private final PromptJudgeService judgeService;
    private final HeuristicPromptEvaluator heuristicEvaluator;

    @Value("${app.evaluation.teaching-miss-cap:55}")
    private int teachingMissCap;

    public PromptEvaluationOrchestrator(PromptJudgeService judgeService,
                                        HeuristicPromptEvaluator heuristicEvaluator) {
        this.judgeService = judgeService;
        this.heuristicEvaluator = heuristicEvaluator;
    }

    public PromptEvaluationResponse evaluate(Challenge challenge, String userPrompt) {
        // An empty submission can never be a meaningful prompt.
        if (userPrompt == null || userPrompt.isBlank()) {
            return emptyPromptResponse(challenge);
        }

        String evaluatedBy = "ai";
        JudgeVerdict verdict = judgeService.judge(challenge, userPrompt);
        if (verdict == null) {
            evaluatedBy = "heuristic";
            verdict = heuristicEvaluator.evaluate(challenge, userPrompt);
        }

        int generalScore = verdict.getClarity() + verdict.getSpecificity() + verdict.getContext();
        int finalScore = computeFinalScore(generalScore, verdict.isTeachingPointMet());

        Map<String, Integer> dimensions = new LinkedHashMap<>();
        dimensions.put("clarity", verdict.getClarity());
        dimensions.put("specificity", verdict.getSpecificity());
        dimensions.put("context", verdict.getContext());

        return PromptEvaluationResponse.builder()
                .score(finalScore)
                .finalScore(finalScore)
                .teachingPoint(teachingPointOf(challenge))
                .teachingPointMet(verdict.isTeachingPointMet())
                .generalScore(generalScore)
                .strengths(verdict.getStrengths())
                .flaws(verdict.getFlaws())
                .improvedPrompt(finalScore >= 90 ? null : verdict.getImprovedPrompt())
                .explanation(verdict.getExplanation())
                .dimensions(dimensions)
                .evaluatedBy(evaluatedBy)
                .build();
    }

    /**
     * Final score is a straight, transparent function of the dimensions, so the
     * number can never contradict the bars. A missed teaching point caps it.
     */
    int computeFinalScore(int dimensionTotal, boolean teachingPointMet) {
        int clampedTotal = Math.max(0, Math.min(MAX_DIMENSION_TOTAL, dimensionTotal));
        int scaled = (int) Math.round(clampedTotal * 100.0 / MAX_DIMENSION_TOTAL);
        if (!teachingPointMet) {
            scaled = Math.min(scaled, clamp(teachingMissCap, 0, 100));
        }
        return clamp(scaled, 0, 100);
    }

    private PromptEvaluationResponse emptyPromptResponse(Challenge challenge) {
        Map<String, Integer> dimensions = new LinkedHashMap<>();
        dimensions.put("clarity", 0);
        dimensions.put("specificity", 0);
        dimensions.put("context", 0);
        return PromptEvaluationResponse.builder()
                .score(0)
                .finalScore(0)
                .teachingPoint(teachingPointOf(challenge))
                .teachingPointMet(false)
                .generalScore(0)
                .strengths(List.of())
                .flaws(List.of("No prompt was submitted."))
                .improvedPrompt(null)
                .explanation("Write a prompt describing what you want the AI to do for this challenge.")
                .dimensions(dimensions)
                .evaluatedBy("system")
                .build();
    }

    private String teachingPointOf(Challenge challenge) {
        if (challenge == null) {
            return null;
        }
        if (challenge.getTopicTaught() != null && !challenge.getTopicTaught().isBlank()) {
            return challenge.getTopicTaught();
        }
        return challenge.getAiEvaluationGuide();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
