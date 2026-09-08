use axum::{extract::State, http::StatusCode, Json};
use std::sync::Arc;
use crate::config::AppConfig;
use crate::models::evaluation::{EvaluationRequest, EvaluationResponse};
use crate::services::judge::JudgeService;

pub async fn evaluate_prompt(
    State(config): State<Arc<AppConfig>>,
    Json(payload): Json<EvaluationRequest>,
) -> Result<Json<EvaluationResponse>, (StatusCode, String)> {
    println!(
        ">>> [ai-evaluator] Received evaluation request for challenge: '{}', prompt length: {}",
        payload.title.as_deref().unwrap_or("Untitled"),
        payload.user_prompt.len()
    );

    if payload.user_prompt.trim().is_empty() {
        return Ok(Json(EvaluationResponse {
            score: 0,
            final_score: 0,
            clarity: 0,
            specificity: 0,
            context: 0,
            teaching_point_met: false,
            strengths: vec![],
            flaws: vec!["Prompt is empty.".into()],
            improved_prompt: String::new(),
            explanation: "No prompt was provided.".into(),
            evaluated_by: "system".into(),
            dimensions: crate::models::evaluation::EvaluationDimensions {
                clarity: 0,
                specificity: 0,
                context: 0,
            },
        }));
    }

    match JudgeService::judge(&config, &payload).await {
        Ok(verdict) => {
            let clarity = verdict.clarity.clamp(0, 33);
            let specificity = verdict.specificity.clamp(0, 33);
            let context = verdict.context.clamp(0, 33);

            let mut raw_score = ((clarity + specificity + context) as f64 / 99.0 * 100.0).round() as i32;
            if !verdict.teaching_point_met && raw_score > 55 {
                raw_score = 55;
            }

            Ok(Json(EvaluationResponse {
                score: raw_score,
                final_score: raw_score,
                clarity,
                specificity,
                context,
                teaching_point_met: verdict.teaching_point_met,
                strengths: verdict.strengths,
                flaws: verdict.flaws,
                improved_prompt: verdict.improved_prompt,
                explanation: verdict.explanation,
                evaluated_by: "rust-llm-service".into(),
                dimensions: crate::models::evaluation::EvaluationDimensions {
                    clarity,
                    specificity,
                    context,
                },
            }))
        }
        Err(err) => Err((
            StatusCode::INTERNAL_SERVER_ERROR,
            format!("Evaluation error: {err}"),
        )),
    }
}