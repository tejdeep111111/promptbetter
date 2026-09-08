use axum::{routing::{get, post}, Router};
use std::sync::Arc;
use crate::config::AppConfig;
use crate::handlers::{evaluate::evaluate_prompt, health::health_check};

pub fn create_router(config: Arc<AppConfig>) -> Router {
    Router::new()
        .route("/health", get(health_check))
        .route("/api/evaluate", post(evaluate_prompt))
        .with_state(config)
}