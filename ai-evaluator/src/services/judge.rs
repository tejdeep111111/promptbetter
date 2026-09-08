use crate::config::AppConfig;
use crate::models::evaluation::{EvaluationRequest, LlmVerdict};

pub struct JudgeService;

impl JudgeService {
    pub async fn judge(
        config: &AppConfig,
        req: &EvaluationRequest,
    ) -> Result<LlmVerdict, Box<dyn std::error::Error + Send + Sync>> {
        let system_prompt = "You are a prompt-engineering examiner. Grade the USER_PROMPT.\n\
            Score clarity, specificity, context (0-33 each).\n\
            Return ONLY valid JSON with keys: clarity, specificity, context, teaching_point_met, strengths, flaws, improved_prompt, explanation.";

        let mut user_msg = String::from("CHALLENGE\n");
        if let Some(val) = &req.domain { user_msg.push_str(&format!("domain: {val}\n")); }
        if let Some(val) = &req.task { user_msg.push_str(&format!("task: {val}\n")); }
        if let Some(val) = &req.teaching_point { user_msg.push_str(&format!("teaching_point: {val}\n")); }
        if let Some(val) = &req.evaluation_guide { user_msg.push_str(&format!("evaluation_guide: {val}\n")); }
        user_msg.push_str(&format!("\nUSER_PROMPT\n{}", req.user_prompt.trim()));

        let body = serde_json::json!({
            "model": config.model,
            "temperature": 0.1,
            "messages": [
                { "role": "system", "content": system_prompt },
                { "role": "user", "content": user_msg }
            ],
            "max_tokens": 1200
        });

        let resp = config
            .http_client
            .post(&config.api_url)
            .header("Authorization", format!("Bearer {}", config.api_key))
            .json(&body)
            .send()
            .await?
            .error_for_status()?;

        let json: serde_json::Value = resp.json().await?;
        let raw = json["choices"][0]["message"]["content"].as_str().unwrap_or("{}");
        let cleaned = raw.trim()
            .trim_start_matches("```json")
            .trim_start_matches("```")
            .trim_end_matches("```")
            .trim();

        let verdict: LlmVerdict = serde_json::from_str(cleaned)?;
        Ok(verdict)
    }
}