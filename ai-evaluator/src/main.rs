mod config;
mod handlers;
mod models;
mod routes;
mod services;

use config::AppConfig;
use std::net::SocketAddr;

#[tokio::main]
async fn main() {
    let config = AppConfig::from_env();
    let app = routes::create_router(config);

    let addr = SocketAddr::from(([0, 0, 0, 0], 8081));
    println!("🚀 Rust AI Evaluator listening on http://localhost:8081");

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}