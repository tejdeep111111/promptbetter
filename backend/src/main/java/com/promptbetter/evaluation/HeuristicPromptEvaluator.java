package com.promptbetter.evaluation;

import com.promptbetter.model.Challenge;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deterministic, domain-agnostic prompt evaluator used when the LLM judge is
 * unavailable. Instead of a dead "temporarily unavailable" zero, it produces a
 * genuinely useful score by measuring observable prompt features and honest,
 * specific feedback. Scoring is intentionally conservative so a real LLM
 * evaluation still tends to score at least as high.
 */
@Component
public class HeuristicPromptEvaluator {

    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9']+");

    private static final Set<String> ROLE_SIGNALS = Set.of(
            "you are", "act as", "as a", "as an", "your role", "persona", "expert", "senior", "professional");
    private static final Set<String> FORMAT_SIGNALS = Set.of(
            "json", "table", "list", "bullet", "bullets", "markdown", "format", "numbered",
            "step-by-step", "step by step", "steps", "code", "csv", "yaml", "paragraph", "headline", "outline");
    private static final Set<String> CONSTRAINT_SIGNALS = Set.of(
            "must", "only", "limit", "at least", "at most", "no more than", "under", "within",
            "words", "characters", "sentences", "include", "exclude", "avoid", "ensure", "require",
            "constraint", "maximum", "minimum", "exactly");
    private static final Set<String> AUDIENCE_SIGNALS = Set.of(
            "audience", "beginner", "beginners", "expert", "experts", "reader", "readers", "customer",
            "customers", "user", "users", "for a", "aimed at", "targeted at", "tone", "voice", "style");

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "to", "of", "in", "on", "for", "with", "that", "this",
            "is", "are", "be", "it", "as", "at", "by", "your", "you", "write", "create", "make",
            "need", "want", "using", "use", "should", "would", "please", "help");

    public JudgeVerdict evaluate(Challenge challenge, String userPrompt) {
        JudgeVerdict verdict = new JudgeVerdict();

        String prompt = userPrompt == null ? "" : userPrompt.trim();
        String lower = prompt.toLowerCase(Locale.ROOT);
        int wordCount = countWords(prompt);

        boolean hasRole = containsAny(lower, ROLE_SIGNALS);
        boolean hasFormat = containsAny(lower, FORMAT_SIGNALS);
        boolean hasConstraints = containsAny(lower, CONSTRAINT_SIGNALS);
        boolean hasAudience = containsAny(lower, AUDIENCE_SIGNALS);
        double taskOverlap = taskOverlap(challenge, lower);

        int clarity = scoreClarity(wordCount, prompt);
        int specificity = scoreSpecificity(wordCount, hasFormat, hasConstraints, taskOverlap);
        int context = scoreContext(hasRole, hasAudience, taskOverlap);

        verdict.setClarity(clarity);
        verdict.setSpecificity(specificity);
        verdict.setContext(context);

        List<String> strengths = new ArrayList<>();
        List<String> flaws = new ArrayList<>();

        if (wordCount >= 25) {
            strengths.add("The prompt gives a substantial amount of detail to work with.");
        }
        if (hasRole) {
            strengths.add("It frames a role or persona for the AI to adopt.");
        } else {
            flaws.add("No role or persona is set for the AI (e.g. \"You are a senior ...\").");
        }
        if (hasFormat) {
            strengths.add("It specifies a desired output format or structure.");
        } else {
            flaws.add("The desired output format or structure is not specified.");
        }
        if (hasConstraints) {
            strengths.add("It includes explicit constraints or requirements.");
        } else {
            flaws.add("No explicit constraints (length, must-haves, edge cases) are given.");
        }
        if (!hasAudience) {
            flaws.add("The target audience or tone is not defined.");
        }
        if (taskOverlap < 0.15) {
            flaws.add("The prompt does not clearly reflect the specifics of the task.");
        }
        if (wordCount < 8) {
            flaws.add("The prompt is very short, leaving the request under-specified.");
        }
        if (strengths.isEmpty()) {
            strengths.add("The prompt states a basic request that can be built upon.");
        }

        verdict.setStrengths(strengths);
        verdict.setFlaws(flaws);
        verdict.setTeachingPointMet(hasRole && hasConstraints && taskOverlap >= 0.2);
        verdict.setImprovedPrompt(buildImprovedPrompt(challenge));
        verdict.setExplanation(
                "Scored automatically from measurable prompt features (detail, role, "
                + "output format, constraints, audience and relevance to the task). "
                + "Add the missing elements listed under flaws to raise the score.");

        return verdict;
    }

    private int scoreClarity(int wordCount, String prompt) {
        if (wordCount == 0) {
            return 0;
        }
        int score = 6;
        if (wordCount >= 6) score += 6;
        if (wordCount >= 15) score += 6;
        if (wordCount >= 30) score += 4;
        // A prompt with sentence structure reads more clearly.
        if (prompt.matches("(?s).*[.!?].*")) score += 5;
        if (wordCount >= 8 && wordCount <= 120) score += 3;
        return clamp(score);
    }

    private int scoreSpecificity(int wordCount, boolean hasFormat, boolean hasConstraints, double taskOverlap) {
        int score = 3;
        if (wordCount >= 15) score += 4;
        if (hasFormat) score += 9;
        if (hasConstraints) score += 10;
        if (taskOverlap >= 0.3) score += 5;
        else if (taskOverlap >= 0.15) score += 2;
        return clamp(score);
    }

    private int scoreContext(boolean hasRole, boolean hasAudience, double taskOverlap) {
        int score = 3;
        if (hasRole) score += 13;
        if (hasAudience) score += 10;
        if (taskOverlap >= 0.3) score += 5;
        else if (taskOverlap >= 0.15) score += 2;
        return clamp(score);
    }

    /** Fraction of the challenge's meaningful task words that appear in the prompt. */
    private double taskOverlap(Challenge challenge, String lowerPrompt) {
        if (challenge == null || challenge.getTask() == null || lowerPrompt.isBlank()) {
            return 0.0;
        }
        Set<String> taskWords = new LinkedHashSet<>();
        for (String w : WORD.matcher(challenge.getTask().toLowerCase(Locale.ROOT)).results()
                .map(m -> m.group()).toList()) {
            if (w.length() > 3 && !STOP_WORDS.contains(w)) {
                taskWords.add(w);
            }
        }
        if (taskWords.isEmpty()) {
            return 0.0;
        }
        Set<String> promptWords = new LinkedHashSet<>(
                Arrays.asList(WORD.matcher(lowerPrompt).results().map(m -> m.group()).toArray(String[]::new)));
        int hits = 0;
        for (String w : taskWords) {
            if (promptWords.contains(w)) {
                hits++;
            }
        }
        return (double) hits / taskWords.size();
    }

    private String buildImprovedPrompt(Challenge challenge) {
        String task = challenge == null || challenge.getTask() == null
                ? "the requested output" : challenge.getTask().trim();
        String constraints = challenge == null ? null : challenge.getConstraintAsString();
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert in ")
          .append(challenge != null && challenge.getDomain() != null ? challenge.getDomain() : "this field")
          .append(". ").append(task);
        if (constraints != null && !constraints.isBlank()) {
            sb.append(" Constraints: ").append(constraints.trim()).append('.');
        }
        sb.append(" Specify the exact output format, the target audience and any edge cases,"
                + " and keep the response focused and concrete.");
        return sb.toString();
    }

    private boolean containsAny(String haystack, Set<String> needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) WORD.matcher(text).results().count();
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(33, value));
    }
}
