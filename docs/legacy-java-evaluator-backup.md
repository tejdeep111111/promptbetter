# Legacy Java Evaluation Engine Backup

This document preserves the original in-process Java prompt evaluation implementation before migrating `SubmissionService` to the standalone Rust `ai-evaluator` microservice on port 8081.

---

## 1. `PromptJudgeService.java`
**Location:** `backend/src/main/java/com/promptbetter/evaluation/PromptJudgeService.java`

```java
package com.promptbetter.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptbetter.model.Challenge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Judges a user's prompt against the ACTUAL challenge it was written for,
 * using a single LLM call with an anchored rubric. This is what makes the
 * evaluation meaningful: the model scores the prompt relative to the task,
 * the taught skill and the challenge constraints — not a fixed coding
 * checklist.
 *
 * <p>Returns {@code null} on any failure so the orchestrator can fall back
 * to the deterministic {@link HeuristicPromptEvaluator}.</p>
 */
@Service
public class PromptJudgeService {

    private static final Logger log = LoggerFactory.getLogger(PromptJudgeService.class);

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.base-url}")
    private String apiUrl;

    @Value("${ai.model}")
    private String model;

    @Value("${app.evaluation.judge-temperature:0.1}")
    private double temperature;

    private static final String SYSTEM_PROMPT = """
            You are a strict, fair prompt-engineering examiner. You grade how well a
            USER_PROMPT would instruct an AI to accomplish a specific CHALLENGE.

            You judge the PROMPT itself — its clarity, detail and framing — NOT whether
            you personally could do the task. A vague one-line request must score low even
            if the task is easy.

            Score three dimensions, each an integer from 0 to 33, based ONLY on evidence
            present in the USER_PROMPT relative to the CHALLENGE:

            clarity (0-33): Is the objective unambiguous, well-structured and easy to
              follow? Anchors: 0-8 confusing/empty, 9-16 vague, 17-24 mostly clear,
              25-33 crisp and unambiguous.
            specificity (0-33): Concrete requirements, constraints, desired output
              format, examples, quantities, edge cases. Anchors: 0-8 none, 9-16 minimal,
              17-24 several concrete details, 25-33 thorough and precise.
            context (0-33): Role/persona, target audience, tone and background framing
              appropriate to this challenge. Anchors: 0-8 none, 9-16 slight, 17-24 good
              framing, 25-33 rich, well-targeted context.

            Also decide teaching_point_met: does the prompt actually demonstrate the
            skill named in TEACHING_POINT / EVALUATION_GUIDE? Be honest.

            Rules:
            - Be domain-appropriate. Do NOT demand a programming language for a writing
              or analysis task. Judge what THIS challenge needs.
            - strengths and flaws must be specific to THIS prompt, not generic filler.
              1-4 items each. If the prompt is strong, flaws may be short.
            - improved_prompt: a rewritten version that would score near the top for this
              exact challenge.
            - Do NOT output a score number; only the three dimensions drive it.

            Return ONLY minified JSON, no markdown, with EXACTLY these keys:
            {"clarity":0,"specificity":0,"context":0,"teaching_point_met":false,
             "strengths":["..."],"flaws":["..."],"improved_prompt":"...","explanation":"..."}
            """;

    private RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @return a parsed verdict, or {@code null} if the LLM call or parsing failed.
     */
    public JudgeVerdict judge(Challenge challenge, String userPrompt) {
        try {
            String userMessage = buildUserMessage(challenge, userPrompt);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", temperature,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "max_tokens", 1200
            );

            String raw = callApi(body);
            JudgeVerdict verdict = objectMapper.readValue(sanitize(raw), JudgeVerdict.class);
            normalize(verdict);
            return verdict;
        } catch (Exception e) {
            log.warn("LLM judge failed for challenge id={}, will fall back to heuristic",
                    challenge == null ? null : challenge.getId(), e);
            return null;
        }
    }

    private String buildUserMessage(Challenge challenge, String userPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("CHALLENGE\n");
        append(sb, "domain", challenge.getDomain());
        append(sb, "title", challenge.getTitle());
        append(sb, "difficulty", challenge.getHardness());
        append(sb, "task", challenge.getTask());
        append(sb, "teaching_point", firstNonBlank(challenge.getTopicTaught(), challenge.getAiEvaluationGuide()));
        append(sb, "evaluation_guide", challenge.getAiEvaluationGuide());
        append(sb, "required_constraints", challenge.getConstraintAsString());
        append(sb, "key_takeaway", challenge.getKeyTakeaway());
        sb.append("\nUSER_PROMPT\n");
        sb.append(userPrompt == null ? "" : userPrompt.trim());
        return sb.toString();
    }

    private void append(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(value.trim()).append('\n');
        }
    }

    private void normalize(JudgeVerdict verdict) {
        verdict.setClarity(clamp(verdict.getClarity()));
        verdict.setSpecificity(clamp(verdict.getSpecificity()));
        verdict.setContext(clamp(verdict.getContext()));
        if (verdict.getStrengths() == null) {
            verdict.setStrengths(new ArrayList<>());
        }
        if (verdict.getFlaws() == null) {
            verdict.setFlaws(new ArrayList<>());
        }
        if (verdict.getImprovedPrompt() == null) {
            verdict.setImprovedPrompt("");
        }
        if (verdict.getExplanation() == null) {
            verdict.setExplanation("");
        }
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(33, value));
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    private String callApi(Map<String, Object> body) throws Exception {
        JsonNode json = restClient.post()
                .uri(apiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        return json.path("choices").get(0).path("message").path("content").asText();
    }

    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        String cleaned = raw.replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }
}
```

---

## 2. `PromptEvaluationOrchestrator.java`
**Location:** `backend/src/main/java/com/promptbetter/evaluation/PromptEvaluationOrchestrator.java`

```java
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
 */
@Service
public class PromptEvaluationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PromptEvaluationOrchestrator.class);

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
        if (userPrompt == null || userPrompt.isBlank()) {
            return emptyPromptResponse(challenge);
        }

        String evaluatedBy = "ai";
        JudgeVerdict verdict = judgeService.judge(challenge, userPrompt);
        if (verdict == null) {
            log.info("Falling back to deterministic heuristic evaluator for challenge id={}",
                    challenge == null ? null : challenge.getId());
            verdict = heuristicEvaluator.evaluate(challenge, userPrompt);
            evaluatedBy = "heuristic";
        }

        int clarity = verdict.getClarity();
        int specificity = verdict.getSpecificity();
        int context = verdict.getContext();

        int generalScore = clarity + specificity + context;
        int rawFinal = (int) Math.round(((double) generalScore / MAX_DIMENSION_TOTAL) * 100.0);

        int finalScore = rawFinal;
        if (!verdict.isTeachingPointMet() && finalScore > teachingMissCap) {
            finalScore = teachingMissCap;
        }

        Map<String, Integer> dimensions = new LinkedHashMap<>();
        dimensions.put("clarity", clarity);
        dimensions.put("specificity", specificity);
        dimensions.put("context", context);

        String teachingPoint = challenge == null ? ""
                : (challenge.getTopicTaught() != null ? challenge.getTopicTaught() : challenge.getTitle());

        return PromptEvaluationResponse.builder()
                .score(finalScore)
                .finalScore(finalScore)
                .teachingPoint(teachingPoint)
                .teachingPointMet(verdict.isTeachingPointMet())
                .generalScore(generalScore)
                .strengths(verdict.getStrengths())
                .flaws(verdict.getFlaws())
                .improvedPrompt(verdict.getImprovedPrompt())
                .explanation(verdict.getExplanation())
                .dimensions(dimensions)
                .evaluatedBy(evaluatedBy)
                .build();
    }

    private PromptEvaluationResponse emptyPromptResponse(Challenge challenge) {
        Map<String, Integer> zeroDimensions = Map.of(
                "clarity", 0,
                "specificity", 0,
                "context", 0
        );

        return PromptEvaluationResponse.builder()
                .score(0)
                .finalScore(0)
                .teachingPoint(challenge == null ? "" : challenge.getTitle())
                .teachingPointMet(false)
                .generalScore(0)
                .strengths(List.of())
                .flaws(List.of("No prompt was provided to evaluate."))
                .improvedPrompt("")
                .explanation("Enter a prompt that attempts the challenge task to receive feedback.")
                .dimensions(zeroDimensions)
                .evaluatedBy("system")
                .build();
    }
}
```

---

## 3. Original `SubmissionService.java`
**Location:** `backend/src/main/java/com/promptbetter/service/SubmissionService.java`

```java
package com.promptbetter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptbetter.evaluation.PromptEvaluationOrchestrator;
import com.promptbetter.evaluation.PromptEvaluationResponse;
import com.promptbetter.model.Challenge;
import com.promptbetter.model.Submission;
import com.promptbetter.model.UserProgress;
import com.promptbetter.repository.ChallengeRepository;
import com.promptbetter.repository.SubmissionRepository;
import com.promptbetter.repository.UserProgressRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SubmissionService {
    private final SubmissionRepository submissionRepository;
    private final ChallengeRepository challengeRepository;
    private final UserProgressRepository userProgressRepository;
    private final PromptEvaluationOrchestrator evaluationOrchestrator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> submitPrompt(Long userId, Long challengeId, String prompt) throws Exception {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        PromptEvaluationResponse evaluationResponse = evaluationOrchestrator.evaluate(challenge, prompt);
        String feedbackJson;
        try {
            feedbackJson = objectMapper.writeValueAsString(evaluationResponse);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize evaluation response", e);
        }
        int score = evaluationResponse.getFinalScore();

        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setChallengeId(challengeId);
        submission.setUserPrompt(prompt);
        submission.setScore(score);
        submission.setFeedback(feedbackJson);
        submissionRepository.save(submission);

        boolean leveledUp = updateUserProgress(userId, challenge.getDomain(), challenge.getLevel(), score);

        return Map.of(
                "score", score,
                "feedback", objectMapper.readTree(feedbackJson),
                "leveledUp", leveledUp,
                "nextLevel", challenge.getLevel() + (leveledUp ? 1 : 0)
        );
    }

    private boolean updateUserProgress(Long userId, String domain, int completedLevel, int score) {
        Optional<UserProgress> existingProgressOpt = userProgressRepository.findByUserIdAndDomain(userId, domain);
        UserProgress progress = existingProgressOpt.orElseGet(() -> {
            UserProgress newProgress = new UserProgress();
            newProgress.setUserId(userId);
            newProgress.setDomain(domain);
            return newProgress;
        });

        boolean leveledUp = false;
        progress.setXp(progress.getXp() + score);
        if (score > 70 && progress.getCurrentLevel() <= completedLevel) {
            progress.setCurrentLevel(completedLevel + 1);
            leveledUp = true;
        }

        userProgressRepository.save(progress);
        return leveledUp;
    }

    public Optional<Map<String, Object>> getLatestSubmission(Long userID, Long challengeId) {
        Optional<Submission> latestOptional = submissionRepository.findTopByUserIdAndChallengeIdOrderByCreatedAtDesc(userID, challengeId);

        if (latestOptional.isEmpty()) {
            return Optional.empty();
        }

        Submission latest = latestOptional.get();

        try {
            JsonNode feedback = objectMapper.readTree(latest.getFeedback());
            return Optional.of(Map.of(
                    "id", latest.getId(),
                    "score", latest.getScore(),
                    "userPrompt", latest.getUserPrompt(),
                    "feedback", feedback,
                    "createdAt", latest.getCreatedAt().toString()
            ));
        } catch (Exception e) {
            Map<String, Object> fallbackFeedback = Map.of(
                    "score", latest.getScore(),
                    "strengths", List.of(),
                    "flaws", List.of(),
                    "improved_prompt", "",
                    "explanation", "Feedback unavailable",
                    "dimensions", Map.of(
                            "clarity", 0,
                            "context", 0,
                            "specificity", 0,
                            "constraints", 0,
                            "technique", 0
                    )
            );

            return Optional.of(Map.of(
                    "id", latest.getId(),
                    "score", latest.getScore(),
                    "userPrompt", latest.getUserPrompt(),
                    "feedback", fallbackFeedback,
                    "createdAt", latest.getCreatedAt().toString()
            ));
        }
    }
}
```
