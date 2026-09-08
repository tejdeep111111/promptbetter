package com.promptbetter.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptbetter.model.Challenge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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

    @Value("${api.key:}")
    private String apiKey;

    @Value("${api.base-url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.model:openai/gpt-oss-20b}")
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

    //whats the error
    private final RestClient restClient = RestClient.create();
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
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON); //Tells the server the payload format
//        headers.setBearerAuth(apiKey); //Authenticates the request
//
//        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers); //Wraps the body and headers into a single object
//
//        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class); //Sends the HTTP POST request and returns the response synchronously
//
//        JsonNode root = objectMapper.readTree(response.getBody());
//        return root.path("choices").get(0).path("message").path("content").asText();
    }

    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        String cleaned = raw.replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();
        // Keep only the outermost JSON object in case the model adds prose.
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }
}
