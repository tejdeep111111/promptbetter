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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@Service
public class SubmissionService {
    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    private final SubmissionRepository submissionRepository;
    private final ChallengeRepository challengeRepository;
    private final UserProgressRepository userProgressRepository;
    private final PromptEvaluationOrchestrator evaluationOrchestrator;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.evaluator.url:http://localhost:8081/api/evaluate}")
    private String evaluatorUrl;

    public SubmissionService(SubmissionRepository submissionRepository,
                             ChallengeRepository challengeRepository,
                             UserProgressRepository userProgressRepository,
                             PromptEvaluationOrchestrator evaluationOrchestrator) {
        this.submissionRepository = submissionRepository;
        this.challengeRepository = challengeRepository;
        this.userProgressRepository = userProgressRepository;
        this.evaluationOrchestrator = evaluationOrchestrator;
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> submitPrompt(Long userId, Long challengeId, String prompt) throws Exception {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        int score;
        String feedbackJson;

        try {
            // Call the Rust AI Evaluator microservice
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("user_prompt", prompt);
            requestPayload.put("domain", challenge.getDomain());
            requestPayload.put("title", challenge.getTitle());
            requestPayload.put("task", challenge.getTask());
            requestPayload.put("difficulty", challenge.getHardness() != null ? challenge.getHardness() : "");
            requestPayload.put("teaching_point", challenge.getTopicTaught() != null ? challenge.getTopicTaught() : challenge.getAiEvaluationGuide());
            requestPayload.put("evaluation_guide", challenge.getAiEvaluationGuide());

            JsonNode evalResponse = restClient.post()
                    .uri(evaluatorUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(JsonNode.class);

            log.info("Successfully evaluated prompt via Rust microservice at {}. Score: {}", evaluatorUrl, evalResponse.path("score").asInt());
            score = evalResponse.path("score").asInt();
            feedbackJson = objectMapper.writeValueAsString(evalResponse);
        } catch (Exception ex) {
            log.warn("Rust evaluator microservice call failed at {}. Falling back to in-process orchestrator.", evaluatorUrl, ex);
            PromptEvaluationResponse evaluationResponse = evaluationOrchestrator.evaluate(challenge, prompt);
            score = evaluationResponse.getFinalScore();
            feedbackJson = objectMapper.writeValueAsString(evaluationResponse);
        }

        // Save submission and update user progress
        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setChallengeId(challengeId);
        submission.setUserPrompt(prompt);
        submission.setScore(score);
        submission.setFeedback(feedbackJson);
        submissionRepository.save(submission);

        // Step 4: Update user progress (level up if score >= 70)
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
        // Level up logic: if score is above 70 and user has completed the current level, move to the next level
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
            // Fallback: return a default feedback structure the frontend can handle
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
