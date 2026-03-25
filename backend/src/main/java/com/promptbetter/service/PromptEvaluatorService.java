package com.promptbetter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class PromptEvaluatorService {

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.base-url}")
    private String apiUrl;

    @Value("${ai.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String EVALUATOR_SYSTEM_PROMPT = """
        You are an expert Prompt Engineering coach.

        You will be given:
        1. SCENARIO: A real-world task the user is trying to solve
        2. USER_PROMPT: The prompt the user wrote to solve that scenario

        Evaluate the quality of the USER_PROMPT based on prompt engineering best practices
        and return ONLY valid JSON (no markdown, no extra text) in this exact format:
        {
          "score": <integer 0-100>,
          "strengths": ["<strength1>", "<strength2>"],
          "flaws": ["<flaw1>", "<flaw2>"],
          "improved_prompt": "<a significantly better version of the user's prompt>",
          "explanation": "<2-3 sentence summary of the evaluation>",
          "dimensions": {
            "clarity": <0-20>,
            "context": <0-20>,
            "specificity": <0-20>,
            "constraints": <0-20>,
            "technique": <0-20>
          }
        }
    """;

    public String evaluatePrompt(String scenario, String userPrompt, String idealPrompt) {
        return evaluatePrompt(scenario, userPrompt);
    }

    public String evaluatePrompt(String scenario, String userPrompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String userMessage = String.format("""
                SCENARIO: %s
                USER_PROMPT: %s

                Evaluate the USER_PROMPT and return the JSON evaluation.""", scenario, userPrompt);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", EVALUATOR_SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "max_tokens", 800
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            return "{\"score\": 0, \"strengths\": [], \"flaws\": [\"Evaluation failed\"], \"improved_prompt\": \"\", \"explanation\": \"" + e.getMessage() + "\"}";
        }
    }
}