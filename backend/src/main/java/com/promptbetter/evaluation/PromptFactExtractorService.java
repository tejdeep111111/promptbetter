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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

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
            You are a precise prompt analyst. Extract facts that are EXPLICITLY stated
            in the USER_PROMPT. Be generous when detecting programming languages — if the
            user mentions any language name (e.g. Java, Python, JavaScript, C++, etc.)
            anywhere in the prompt, set programming_language to that language.

            Return ONLY valid JSON with this schema:
            {
              "programming_language": "<language name if ANY programming language is mentioned, else null>",
              "ai_role_defined": <true if the prompt assigns a role/persona to the AI, else false>,
              "output_format_specified": <true if format like JSON/table/list/code/numbered is mentioned, else false>,
              "return_type_specified": <true if return type or output data type is mentioned, else false>,
              "placeholders_used": <true if <PLACEHOLDER> style variables are present, else false>,
              "length_constraint": "<exact length/word/line constraint mentioned or null>",
              "audience_defined": "<target audience or experience level if mentioned, or null>",
              "step_by_step_requested": <true if step-by-step or chain-of-thought is requested, else false>,
              "explicit_constraints": "<any other explicit constraints or requirements, or null>"
            }
            """;

    /** Known languages for local keyword fallback detection. */
    private static final Map<Pattern, String> LANGUAGE_PATTERNS = Map.ofEntries(
            entry("java"), entry("python"), entry("javascript"), entry("typescript"),
            entry("c\\+\\+", "C++"), entry("c#", "C#"), entry("csharp", "C#"),
            entry("ruby"), entry("go"), entry("golang", "Go"), entry("rust"),
            entry("kotlin"), entry("swift"), entry("php"), entry("scala"),
            entry("perl"), entry("r"), entry("dart"), entry("lua"),
            entry("haskell"), entry("elixir"), entry("clojure")
    );

    private static Map.Entry<Pattern, String> entry(String keyword) {
        return entry(keyword, keyword.substring(0, 1).toUpperCase() + keyword.substring(1));
    }

    private static Map.Entry<Pattern, String> entry(String regex, String displayName) {
        return Map.entry(
                Pattern.compile("(?i)\\b" + regex + "\\b"),
                displayName
        );
    }

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FactSheet extractFacts(String userPrompt) {
        FactSheet factSheet;
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
            factSheet = objectMapper.readValue(sanitize(raw), FactSheet.class);
        } catch (Exception e) {
            log.warn("Fact extraction failed, using empty fact sheet fallback", e);
            factSheet = new FactSheet();
        }

        // Local keyword fallback: if the LLM missed a programming language, detect it ourselves
        if (factSheet.getProgrammingLanguage() == null || factSheet.getProgrammingLanguage().isBlank()) {
            String detected = detectLanguageLocally(userPrompt);
            if (detected != null) {
                log.info("Local fallback detected programming language '{}' that LLM missed", detected);
                factSheet.setProgrammingLanguage(detected);
            }
        }

        return factSheet;
    }

    /**
     * Scans the prompt for well-known programming language keywords.
     */
    private String detectLanguageLocally(String prompt) {
        if (prompt == null) return null;
        for (Map.Entry<Pattern, String> entry : LANGUAGE_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(prompt).find()) {
                return entry.getValue();
            }
        }
        return null;
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
