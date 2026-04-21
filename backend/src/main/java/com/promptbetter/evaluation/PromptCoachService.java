package com.promptbetter.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PromptCoachService {
    private static final Logger log = LoggerFactory.getLogger(PromptCoachService.class);

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.base-url}")
    private String apiUrl;

    @Value("${ai.model}")
    private String model;

    @Value("${app.evaluation.coach-temperature:0.2}")
    private double temperature;

    private static final String COACH_SYSTEM_PROMPT = """
            You are a friendly prompt coach helping everyday users improve.
            You will receive a TASK, USER_PROMPT, FACT_SHEET and TEACHING_POINT status.

            IMPORTANT: The FACT_SHEET is the authoritative source of truth about what the
            user's prompt contains. If FACT_SHEET shows programming_language is set (not null),
            then the user DID specify a programming language — do NOT list it as a flaw.
            Similarly, trust TEACHING_POINT_MET: if true, do not contradict it in flaws.
            Only flag things as flaws if the FACT_SHEET confirms they are missing.

            Generate coaching feedback and general quality dimensions only.
            Do not calculate final score or weighted score.

            Dimensions must be integers from 0 to 33 for:
            clarity    — how clear and unambiguous the prompt is
            specificity — how detailed, constrained and precise the prompt is
            context     — how well the prompt sets role, audience, technique and framing

            Return ONLY JSON:
            {
              "strengths": ["..."],
              "flaws": ["..."],
              "improved_prompt": "...",
              "explanation": "...",
              "dimensions": {
                "clarity": 0,
                "specificity": 0,
                "context": 0
              }
            }
            """;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PromptCoachFeedback generateFeedback(String task,
                                                String userPrompt,
                                                FactSheet factSheet,
                                                TeachingPointRule rule,
                                                TeachingPointScorer.TeachingPointResult teachingResult) {
        try {
            String userMessage = "TASK: " + task + "\n\n"
                    + "USER_PROMPT: " + userPrompt + "\n\n"
                    + "FACT_SHEET: " + objectMapper.writeValueAsString(factSheet) + "\n\n"
                    + "TEACHING_POINT: " + (rule == null ? "" : rule.getTeachingPoint()) + "\n"
                    + "TEACHING_POINT_MET: " + teachingResult.met() + "\n"
                    + "MISSED_FACTS: " + teachingResult.missedFacts();

            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", temperature,
                    "messages", List.of(
                            Map.of("role", "system", "content", COACH_SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "max_tokens", 1200
            );

            String raw = callApi(body);
            PromptCoachFeedback parsed = objectMapper.readValue(sanitize(raw), PromptCoachFeedback.class);
            parsed.setDimensions(normalizeDimensions(parsed.getDimensions()));
            return parsed;
        } catch (Exception e) {
            log.warn("Coach feedback generation failed, returning fallback feedback", e);
            PromptCoachFeedback fallback = new PromptCoachFeedback();
            fallback.setFlaws(List.of("Evaluation feedback is temporarily unavailable."));
            fallback.setExplanation("Your submission was scored with deterministic backend rules. Please try again for richer coaching.");
            fallback.setDimensions(normalizeDimensions(null));
            return fallback;
        }
    }

    private Map<String, Integer> normalizeDimensions(Map<String, Integer> rawDimensions) {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        normalized.put("clarity", clamp(rawDimensions == null ? 0 : rawDimensions.getOrDefault("clarity", 0), 0, 33));
        normalized.put("specificity", clamp(rawDimensions == null ? 0 : rawDimensions.getOrDefault("specificity", 0), 0, 33));
        normalized.put("context", clamp(rawDimensions == null ? 0 : rawDimensions.getOrDefault("context", 0), 0, 33));
        return normalized;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String callApi(Map<String, Object> body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    private String sanitize(String raw) {
        if (raw == null) {
            return "{}";
        }
        return raw.replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();
    }
}
