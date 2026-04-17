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

import java.util.List;
import java.util.Map;

@Service
public class PromptFactExtractorService {
    private static final Logger log = LoggerFactory.getLogger(PromptFactExtractorService.class);

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.base-url}")
    private String apiUrl;

    @Value("${ai.model}")
    private String model;

    @Value("${app.evaluation.fact-extractor-temperature:0.1}")
    private double temperature;

    private static final String EXTRACTOR_SYSTEM_PROMPT = """
            You are a precise prompt analyst. Extract only explicit facts from USER_PROMPT.
            Do not infer or assume missing details.

            Return ONLY valid JSON with this schema:
            {
              "programming_language": "<exact language mentioned or null>",
              "ai_role_defined": <true/false>,
              "output_format_specified": <true/false>,
              "return_type_specified": <true/false>,
              "placeholders_used": <true/false>,
              "length_constraint": "<exact length/word/line constraint or null>",
              "audience_defined": "<target audience or null>",
              "step_by_step_requested": <true/false>,
              "explicit_constraints": "<other explicit constraints or null>"
            }
            """;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FactSheet extractFacts(String userPrompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", temperature,
                    "messages", List.of(
                            Map.of("role", "system", "content", EXTRACTOR_SYSTEM_PROMPT),
                            Map.of("role", "user", "content", "USER_PROMPT:\n" + userPrompt)
                    ),
                    "max_tokens", 350
            );

            String raw = callApi(body);
            return objectMapper.readValue(sanitize(raw), FactSheet.class);
        } catch (Exception e) {
            log.warn("Fact extraction failed, using empty fact sheet fallback", e);
            return new FactSheet();
        }
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
