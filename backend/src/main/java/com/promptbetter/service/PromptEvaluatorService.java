//package com.promptbetter.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class PromptEvaluatorService {
//
//    @Value("${api.key}")
//    private String apiKey;
//
//    @Value("${api.base-url}")
//    private String apiUrl;
//
//    @Value("${ai.model}")
//    private String model;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    // ─────────────────────────────────────────────
//    // CALL 1 SYSTEM PROMPT — Fact Extractor
//    // ─────────────────────────────────────────────
//    private static final String EXTRACTOR_SYSTEM_PROMPT = """
//        You are a precise prompt analyst. Your only job is to extract facts that are
//        EXPLICITLY present in the given prompt. Do not infer. Do not assume.
//        If something is not clearly written — mark it as null or false.
//
//        Return ONLY valid JSON, no markdown, no extra text:
//        {
//          "programming_language": "<exact language mentioned or null>",
//          "ai_role_defined": <true if 'You are a...' or similar persona assigned to AI, else false>,
//          "output_format_specified": <true if format like JSON/table/list/numbered is mentioned, else false>,
//          "return_type_specified": <true if return type or output value is mentioned, else false>,
//          "placeholders_used": <true if <PLACEHOLDER> style variables are present, else false>,
//          "length_constraint": "<exact length/word/line constraint mentioned or null>",
//          "audience_defined": "<target audience explicitly mentioned or null>",
//          "step_by_step_requested": <true if user asks for steps/walkthrough/reasoning, else false>,
//          "explicit_constraints": "<any other explicit constraints mentioned or null>"
//        }
//    """;
//
//    // ─────────────────────────────────────────────
//    // CALL 2 SYSTEM PROMPT — Evaluator
//    // ─────────────────────────────────────────────
//    private static final String EVALUATOR_SYSTEM_PROMPT = """
//        You are a Prompt Engineering coach evaluating prompts written by everyday users — not experts.
//        Be encouraging but honest. Help users grow, don't discourage them.
//
//        You will receive:
//        - TASK: The real-world problem the user wants to solve
//        - USER_PROMPT: The prompt they wrote
//        - EVALUATION_CRITERIA: The single most important thing to check for this specific task
//        - FACT_SHEET: A verified JSON extract of what is explicitly present in the prompt
//
//        CRITICAL — FACT SHEET IS GROUND TRUTH:
//        The FACT_SHEET was extracted by a dedicated analyzer. It is accurate.
//        You MUST NOT contradict it under any circumstances.
//        Base ALL flaws and strengths strictly on the fact sheet. Do not re-analyze the original prompt.
//
//        IMPORTANT DISTINCTIONS:
//        - Role prompting = assigning a persona TO THE AI, also accept if the user mentions his level of understanding.(e.g. "You are an expert in...").
//          "As a developer, explain X" is the user's perspective — NOT AI role prompting.
//        - Evaluate the INTENT behind criteria, not literal keyword matching.
//          e.g. "Think step-by-step" criteria is met if step_by_step_requested = true in fact sheet.
//        - If EVALUATION_CRITERIA is empty or missing → evaluate on general prompt engineering best practices.
//
//        EVALUATION_CRITERIA USAGE:
//        - The EVALUATION_CRITERIA defines the MOST IMPORTANT signal for this specific task.
//        - If criteria is NOT met → major flaw, cap the score at 65 regardless of other dimensions.
//        - If criteria IS met → strong foundation, but still evaluate all 5 dimensions normally.
//        - Meeting criteria alone does NOT guarantee a high score.
//        - Not meeting it does NOT mean ignoring everything else the user did well.
//
//        SCORING (regular users, not PE experts):
//        - 0-30:  Barely usable. No context, no constraints.
//        - 31-60: Shows intent but missing most key elements.
//        - 61-80: Average. Criteria met.
//        - 81-90: Strong. Criteria met + most elements well-covered and structured.
//        - 91-100: Near-perfect. Criteria met + role + context + specificity + constraints + format + edge cases.
//                  VERY RARE. Do not award unless truly exceptional.
//
//        SCORING CONSISTENCY RULE:
//        score = clarity + context + specificity + constraints + technique
//        Always sum dimensions first, then set that as the score. Never compute independently.
//
//        DIMENSION RUBRIC (0-20 each):
//        - clarity:     Is intent immediately obvious with zero ambiguity? (0=unclear, 20=perfect)
//        - context:     Is background/audience/domain defined? (0=none, 20=fully defined)
//        - specificity: Are instructions precise with no room to guess? (0=vague, 20=exact)
//        - constraints: Is format/length/tone explicitly stated? (0=none, 20=all defined)
//        - technique:   Are PE techniques used — role, examples, placeholders, chain-of-thought? (0=none, 20=multiple)
//
//        CALIBRATION:
//        "explain BST"                                         → score: 20
//        "explain BST to a 10th grader"                        → score: 52
//        "You are a CS teacher. Explain BST to a 10th grader
//         using an analogy. Keep it under 200 words, no code"  → score: 78
//        Prompt with role+context+format+constraints+edge cases → score: 92+
//
//        IMPROVED PROMPT RULES:
//        - If score < 85: return a genuinely improved prompt that adds what was missing.
//        - Must be meaningfully different — ADD new elements, do not just rephrase.
//        - If improved prompt looks 80%+ similar to original → rewrite until genuinely better.
//        - Keep all placeholders exactly as they appeared in the original.
//        - If score >= 85: return exactly "YOU ARE DOING GREAT AS USUAL"
//
//        Return ONLY valid JSON, no markdown, no text outside JSON:
//        {
//          "score": <integer 0-100>,
//          "strengths": ["<specific strength>", ...],
//          "flaws": ["<flaw as opportunity, max 3>", ...],
//          "improved_prompt": "<improved version or YOU ARE DOING GREAT AS USUAL>",
//          "explanation": "<2-3 sentences>",
//          "dimensions": {
//            "clarity": <0-20>,
//            "context": <0-20>,
//            "specificity": <0-20>,
//            "constraints": <0-20>,
//            "technique": <0-20>
//          }
//        }
//    """;
//
//    // PUBLIC ENTRY POINT
//    public String evaluatePrompt(String task, String userPrompt, String evaluationCriteria) {
//        try {
//            String factSheet = extractFacts(userPrompt);
//            return evaluate(task, userPrompt, evaluationCriteria, factSheet);
//        } catch (Exception e) {
//            return fallbackJson(e.getMessage());
//        }
//    }
//
//    //CALL 1 SYSTEM PROMPT — Fact Sheet Extractor
//    public String extractFacts(String userPrompt) {
//        try {
//            String userMessage = "Extract facts from this prompt:\n\n" + userPrompt;
//
//            Map<String, Object> body = Map.of(
//                    "model", model,
//                    "messages", List.of(
//                            Map.of("role", "system", "content", EXTRACTOR_SYSTEM_PROMPT),
//                            Map.of("role", "user", "content", userMessage)
//                    ),
//                    "max_tokens", 300
//            );
//
//            String raw = callApi(body);
//            //validate it's parseable JSON and extract fact sheet
//            objectMapper.readTree(raw);
//            return raw;
//        } catch (Exception e) {
//            //handle parsing errors or API issues
//            return """
//                    {
//                        "programming_language": null,
//                        "ai_role_defined": false,
//                        "output_format_specified": false,
//                        "return_type_specified": false,
//                        "placeholders_used": false,
//                        "length_constraint": null,
//                        "audience_defined": null,
//                        "step_by_step_requested": false,
//                        "explicit_constraints": null
//                    }""";
//        }
//    }
//
//    //CALL 2 EVALUATE USING FACT SHEET
//    private String evaluate(String task, String userPrompt, String evaluationCriteria, String factSheet) {
//        try {
//            // handle empty criteria
//            if (evaluationCriteria == null || evaluationCriteria.isBlank()) {
//                evaluationCriteria = "Evaluate based on general prompt engineering best practices " +
//                        "— clarity, context, specificity, constraints, and technique.";
//            }
//
//            String userMessage = String.format("""
//                TASK: %s
//
//                USER_PROMPT: %s
//
//                EVALUATION_CRITERIA: %s
//
//                FACT_SHEET (verified, treat as ground truth):
//                %s
//
//                Evaluate and return the JSON.""",
//                    task, userPrompt, evaluationCriteria, factSheet);
//
//            Map<String, Object> body = Map.of(
//                    "model", model,
//                    "messages", List.of(
//                            Map.of("role", "system", "content", EVALUATOR_SYSTEM_PROMPT),
//                            Map.of("role", "user", "content", userMessage)
//                    ),
//                    "max_tokens", 1500
//            );
//
//            return sanitize(callApi(body));
//
//        } catch (Exception e) {
//            return fallbackJson(e.getMessage());
//        }
//    }
//
//    private String callApi(Map<String, Object> body) throws Exception {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setBearerAuth(apiKey);
//
//        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
//        ResponseEntity<String> response = restTemplate.exchange(
//                apiUrl, HttpMethod.POST, request, String.class);
//
//        JsonNode root = objectMapper.readTree(response.getBody());
//        return root.path("choices").get(0).path("message").path("content").asText();
//    }
//
//    //Sanitize JSON Response
//    private String sanitize(String raw) {
//        if (raw == null || raw.isBlank()) return fallbackJson("Empty response");
//
//        raw = raw.replaceAll("(?s)```json\\s*", "")
//                .replaceAll("(?s)```\\s*", "")
//                .trim();
//
//        try {
//            objectMapper.readTree(raw);
//            return raw;
//        } catch (Exception e) {
//            return fallbackJson("Malformed JSON: " + e.getMessage());
//        }
//    }
//
//    private String fallbackJson(String reason) {
//        return """
//            {
//              "score": 0,
//              "strengths": [],
//              "flaws": ["Evaluation could not be completed"],
//              "improved_prompt": "",
//              "explanation": "%s",
//              "dimensions": {
//                "clarity": 0,
//                "context": 0,
//                "specificity": 0,
//                "constraints": 0,
//                "technique": 0
//              }
//            }
//            """.formatted(reason.replace("\"", "'"));
//    }
//
//
//
//}