package com.promptbetter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptbetter.model.Challenge;
import com.promptbetter.model.Submission;
import com.promptbetter.model.UserProgress;
import com.promptbetter.repository.ChallengeRepository;
import com.promptbetter.repository.SubmissionRepository;
import com.promptbetter.repository.UserProgressRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SubmissionService {
    private final SubmissionRepository submissionRepository;
    private final ChallengeRepository challengeRepository;
    private final UserProgressRepository userProgressRepository;
    private final PromptEvaluatorService evaluatorService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> submitPrompt(Long userId, Long challengeId, String prompt) throws Exception {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        String feedbackJson = evaluatorService.evaluatePrompt(challenge.getScenario(), prompt, challenge.getIdealPrompt());

        int score = 0;
        try {
            JsonNode feedback = objectMapper.readTree(feedbackJson);
            score = feedback.get("score").asInt(0);
        } catch (Exception ignored) {
        }

        // Save submission and update user progress
        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setChallengeId(challengeId);
        submission.setUserPrompt(prompt);
        submission.setScore(score);
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
}
