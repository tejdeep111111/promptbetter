use std::sync::Arc;

#[derive(Clone)]
pub struct AppConfig {
    pub http_client: reqwest::Client,
    pub api_key: String,
    pub api_url: String,
    pub model: String,
}

impl AppConfig {
    pub fn from_env() -> Arc<Self> {
        Arc::new(Self {
            http_client: reqwest::Client::new(),
            api_key: std::env::var("API_KEY").unwrap_or_default(),
            api_url: std::env::var("API_BASE_URL")
                .unwrap_or_else(|_| "https://api.groq.com/openai/v1/chat/completions".into()),
            model: std::env::var("AI_MODEL")
                .unwrap_or_else(|_| "openai/gpt-oss-20b".into()),
        })
    }
}