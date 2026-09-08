# PromptBetter 



![Tech Stack](https://camo.githubusercontent.com/32d40defc3196dad21ed639db40cd41e9242c2d45fb09ca59a78b8a7b5b2ff49/68747470733a2f2f736b696c6c69636f6e732e6465762f69636f6e733f693d6a6176612c737072696e672c6d7973716c2c6769742c727573742c696465612c706f73746d616e267468656d653d6461726b)

An intelligent, full-stack platform designed to accelerate prompt engineering mastery. Practice real-world domain challenges, receive instant rubric-based evaluations powered by an ultra-fast Rust microservice and Groq LLMs, and level up your skills with interactive learning tools.

---

## Table of Contents

- [Overview & Architecture](#-overview--architecture)
- [Key Features](#-key-features)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Configuration & Environment Variables](#-configuration--environment-variables)
- [Quick Start Guide](#-quick-start-guide)
- [Evaluation Rubric & Engine](#-evaluation-rubric--engine)
- [API Reference](#-api-reference)
- [Database Schema](#-database-schema)
- [Testing & Production](#-testing--production)
- [Contributing](#-contributing)
- [License](#-license)

---

## Overview & Architecture

PromptBetter uses a modern multi-service architecture designed for performance, resilience, and interactive learning:

```text
                                  +-----------------------+
                                  |     Web Browser       |
                                  | Modern SPA (Frontend) |
                                  +-----------+-----------+
                                              |
                                              | HTTP / REST (JWT)
                                              v
+-----------------------+         +-------------------------------+
|     Redis Cache       |<------->|      Spring Boot Backend      |
|  (Domains, Challenges |         |         (Port 8080)           |
|      & Glossary)      |         |  Auth • Challenges • Progress |
+-----------------------+         +---------------+---------------+
                                              |         |
                                     JPA/SQL  |         | HTTP POST /api/evaluate
                                              v         v
+-----------------------+         +-----------+    +-------------------------------+
|       MySQL 8.0       |         | Database  |    |  Rust AI Evaluator Service    |
| Users, Submissions,   |<--------+ (Tables)  |    |         (Port 8081)           |
| Challenges & Progress |                          | Powered by Axum, Tokio, Serde |
+-----------------------+                          +---------------+---------------+
                                                                   |
                                              HTTPS / JSON API     v
                                        +------------------------------------------+
                                        |             Groq AI Cloud                |
                                        | (LLaMA 3.1 / GPT OSS Evaluation Models)  |
                                        +------------------------------------------+
```

### Resilient Dual-Evaluation Pipeline
1. **Primary Evaluator**: The backend proxies prompt evaluations via `RestClient` to the standalone **Rust `ai-evaluator` microservice** on port 8081.
2. **In-Process Fallback**: If the microservice is temporarily unreachable, the backend automatically fails over to an internal **Java evaluation orchestrator** (`PromptJudgeService` with deterministic `HeuristicPromptEvaluator`), guaranteeing zero downtime.

---

##  Key Features

- **🎯 Domain-Driven Progression**: Challenge tracks across multiple disciplines (**Software Engineering**, **Content Writing**, **Data Analysis**) with ascending difficulty tiers.
- **⚡ Rust AI Evaluation Microservice**: High-concurrency async evaluation service built with Rust, Axum, and Tokio for minimal latency and high throughput.
- **📊 3-Dimensional Rubric + Pedagogical Check**: Prompts are rigorously scored on **Clarity** (33 pts), **Specificity** (33 pts), and **Context** (33 pts). If a prompt misses the core lesson taught in that challenge, its final score is gated at 55 to reinforce genuine learning.
- **🚀 Multi-Tier Caching**:
  - **Distributed Redis Cache**: Serializes domain lists, challenges, and glossary terms using Jackson with configurable 20-minute TTL.
  - **HTTP Public Cache-Control**: Client and browser caching headers (1-hour `max-age`) on immutable glossary and challenge data.
- **🃏 Interactive Flashcards & Glossary**: Comprehensive collection of prompt engineering techniques, keywords, and patterns with instant filtering.
- **🧠 Educational Visualizers**:
  - **How LLMs Work**: Interactive explanations of attention mechanisms, context windows, and sampling temperature.
  - **Save Tokens**: Practical strategies for token economy and prompt efficiency.
- **🔐 Robust Security & Rate Limiting**:
  - Stateless JWT token authentication with BCrypt password hashing.
  - Role-based security filter chain (`ROLE_USER`, `ROLE_ADMIN`).
  - Sliding-window user rate limiting to safeguard AI inference quotas.

---

## 🛠Tech Stack

<p align="center">
  <img src="https://camo.githubusercontent.com/32d40defc3196dad21ed639db40cd41e9242c2d45fb09ca59a78b8a7b5b2ff49/68747470733a2f2f736b696c6c69636f6e732e6465762f69636f6e733f693d6a6176612c737072696e672c6d7973716c2c6769742c727573742c696465612c706f73746d616e267468656d653d6461726b" alt="Java, Spring, MySQL, Git, Rust, IntelliJ IDEA, Postman" />
</p>

### Core Services & Technologies

| Layer | Technology | Version | Description |
|---|---|---|---|
| **Backend API** | Java | 17 LTS | Primary business logic & API gateway |
| | Spring Boot | 3.2.0 | Framework (Web, Security, Data JPA, Cache, Actuator) |
| | Spring Security | 6.x | Stateless JWT authentication filter |
| | JJWT | 0.11.5 | Token signing and verification |
| **AI Evaluator** | Rust | 2024 Edition | High-performance async microservice (port 8081) |
| | Axum | 0.7 | Async web framework |
| | Tokio | 1.x | Async multi-threaded runtime |
| | Reqwest & Serde | Latest | JSON serialization & Groq HTTP client |
| **Database & Cache** | MySQL | 8.0+ | Relational data persistence |
| | Redis | 6.0+ | In-memory distributed caching with JSON serializer |
| **AI Provider** | Groq API | Cloud | Ultra-low latency inference (LLaMA 3.1 / GPT-OSS) |
| **Frontend** | Vanilla JS / CSS3 | Modern ES6+ | Zero-dependency, responsive single-page application |

---

## 📁 Project Structure

```text
promptbetter/
├── .mvn/wrapper/                  # Maven Wrapper binaries & properties
├── mvnw / mvnw.cmd               # Maven wrapper executable (Root)
├── README.md                      # Project documentation
├── backend/                       # Spring Boot Application
│   ├── pom.xml                    # Maven dependencies & plugins
│   └── src/
│       ├── main/
│       │   ├── java/com/promptbetter/
│       │   │   ├── PromptBetterApplication.java
│       │   │   ├── config/        # Security, CORS, and Redis Cache configs
│       │   │   ├── controller/    # Auth, Challenge, Submission, Glossary, Progress
│       │   │   ├── dto/           # Request/response records (Auth, Submission, Login)
│       │   │   ├── evaluation/    # In-process judge, heuristics, orchestrator
│       │   │   ├── filters/       # JwtAuthFilter
│       │   │   ├── model/         # User, Challenge, Submission, Progress, Glossary
│       │   │   ├── repository/    # Spring Data JPA repositories
│       │   │   ├── service/       # Business logic, rate limiter, auth service
│       │   │   └── util/          # JwtUtil and EmailValidator
│       │   └── resources/
│       │       ├── application.properties   # Main backend configurations
│       │       └── script.sql               # Database schema and seed data
│       └── test/                  # Controller, service, and model unit tests
├── ai-evaluator/                  # Rust AI Evaluator Microservice
│   ├── Cargo.toml                 # Cargo package & dependencies (Axum, Tokio)
│   └── src/
│       ├── main.rs                # Microservice entry point (Port 8081)
│       ├── config.rs              # Environment variable loader
│       ├── routes.rs              # HTTP routing table
│       ├── handlers/              # /health and /api/evaluate handlers
│       ├── models/                # Evaluation request/response structs
│       └── services/              # Groq API judge implementation
├── frontend/                      # Web Client
│   └── index.html                 # Comprehensive responsive Single Page App
└── docs/                          # Architecture notes and legacy migration docs
```

---

## 📋 Prerequisites

Ensure the following tools are installed on your machine:

- **Java JDK 17** or higher: `java -version`
- **Rust & Cargo** (1.80+ / 2024 edition): `cargo --version`
- **MySQL Server 8.0+**: Running locally on port `3306`
- **Redis Server** (or Memurai for Windows): Running on port `6379`
- **Node.js** (optional, recommended for static file serving): `npx --version`
- **Groq API Key**: Obtain a free API key at [console.groq.com](https://console.groq.com)

---

## ⚙️ Configuration & Environment Variables

The application reads configuration from environment variables. Configure the following keys:

| Environment Variable | Required | Default Value | Description |
|---|:---:|---|---|
| `MYSQL_PASSWORD` | **Yes** | _None_ | Password for MySQL database root user |
| `API_KEY` | **Yes** | _None_ | Groq API authentication key |
| `PROMPTBETTER_JWT_SECRET` | **Yes** | _None_ | Secret key for signing JWT tokens (min 256 bits) |
| `AI_MODEL` | No | `openai/gpt-oss-20b` | LLM model name used for evaluation |
| `API_BASE_URL` | No | `https://api.groq.com/openai/v1/chat/completions` | Groq / OpenAI-compatible endpoint |

### Setting Environment Variables

#### Windows (PowerShell)
```powershell
$env:MYSQL_PASSWORD="your_mysql_password"
$env:API_KEY="gsk_your_groq_api_key"
$env:PROMPTBETTER_JWT_SECRET="YourSuperSecretLongKeyWithAtLeast32Characters!!"
$env:AI_MODEL="openai/gpt-oss-20b"
```

#### Linux / macOS (Bash / Zsh)
```bash
export MYSQL_PASSWORD="your_mysql_password"
export API_KEY="gsk_your_groq_api_key"
export PROMPTBETTER_JWT_SECRET="YourSuperSecretLongKeyWithAtLeast32Characters!!"
export AI_MODEL="openai/gpt-oss-20b"
```

---

## 🚀 Quick Start Guide

Follow these steps to run the complete PromptBetter platform locally.

### 1. Database & Cache Setup

1. Start your **MySQL** and **Redis** servers.
2. Create the database:
   ```sql
   CREATE DATABASE IF NOT EXISTS promptbetter;
   ```
   *(Hibernate will automatically create tables on backend startup, or you can run `backend/src/main/resources/script.sql` for initial seed challenges).*

---

### 2. Start the Rust AI Evaluator Microservice (Port 8081)

```bash
cd ai-evaluator
cargo run
```
> **Verification**: Visit `http://localhost:8081/health` in your browser. It should return `{"status":"ok"}`.

---

### 3. Start the Spring Boot Backend (Port 8080)

From the project root:

#### Windows:
```cmd
.\mvnw.cmd spring-boot:run -f backend/pom.xml
```

#### Linux / macOS:
```bash
./mvnw spring-boot:run -f backend/pom.xml
```

> **Verification**: Check health status at `http://localhost:8080/actuator/health`.

---

### 4. Launch the Frontend Application

You can serve the frontend with any static web server:

```bash
cd frontend
npx serve -l 3000 .
```

Open your browser and navigate to **`http://localhost:3000`** (or open `frontend/index.html` directly).

---

## 🧠 Evaluation Rubric & Engine

Prompt submissions undergo structured multi-factor analysis:

### 1. Scoring Dimensions (0 – 99 points)
| Dimension | Range | Criteria |
|---|:---:|---|
| **Clarity** | 0 – 33 | Readability, directness, and lack of contradictory instructions |
| **Specificity** | 0 – 33 | Precision of inputs, data formats, desired outputs, and edge cases |
| **Context** | 0 – 33 | Persona framing, scenario framing, and problem background |

### 2. The Pedagogical Gate (`teaching_point_met`)
Each challenge teaches a specific prompt engineering concept (e.g., *few-shot prompting, negative constraints, delimiter isolation, chain-of-thought*).

- If the user's prompt **demonstrates** the concept:
  $$\text{Final Score} = \text{Clarity} + \text{Specificity} + \text{Context}$$
- If the user's prompt **fails** to demonstrate the taught concept:
  $$\text{Final Score} = \min(\text{General Score}, 55)$$

### 3. Progression & Leveling
- **Pass Threshold**: A score **$\ge 70$** marks the challenge as cleared.
- **XP Award**: Earn experience points equal to your score on each attempt.
- **Progression**: Passing unlocks the next level within that domain.

---

## 📚 API Reference

### 🔐 Authentication

#### Register a New Account
- **Endpoint**: `POST /api/auth/register`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "name": "Alex Mercer",
    "email": "alex@example.com",
    "password": "Password123!"
  }
  ```
- **Response** `201 Created`:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "id": 1,
    "name": "Alex Mercer",
    "email": "alex@example.com"
  }
  ```

#### Login
- **Endpoint**: `POST /api/auth/login`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "alex@example.com",
    "password": "Password123!"
  }
  ```
- **Response** `200 OK`: Returns the JWT token and user profile.

---

### 🎯 Challenges

#### Get Available Domains
- **Endpoint**: `GET /api/challenges/domains`
- **Access**: Authenticated (`Bearer <token>`)
- **Headers**: Includes HTTP cache header (`Cache-Control: public, max-age=3600`)
- **Response**: `["Software Engineering", "Content Writing", "Data Analysis"]`

#### Get Challenges by Domain
- **Endpoint**: `GET /api/challenges?domain=Software%20Engineering`
- **Access**: Authenticated

#### Get Current Challenge by Level
- **Endpoint**: `GET /api/challenges/current?domain=Software%20Engineering&level=1`
- **Access**: Authenticated

---

### ✍️ Submissions & Evaluation

#### Submit Prompt for Evaluation
- **Endpoint**: `POST /api/submissions`
- **Access**: Authenticated
- **Rate Limit**: Default 1 submission per 10 seconds per user
- **Request Body**:
  ```json
  {
    "challengeId": 1,
    "userPrompt": "Act as a senior software architect. Provide a complete, production-ready Python sorting function that accepts a list of dictionaries..."
  }
  ```
- **Response** `201 Created`:
  ```json
  {
    "score": 88,
    "leveledUp": true,
    "nextLevel": 2,
    "feedback": {
      "score": 88,
      "clarity": 30,
      "specificity": 29,
      "context": 29,
      "teaching_point_met": true,
      "strengths": [
        "Explicit role specification provided",
        "Clear constraint on return types"
      ],
      "flaws": [
        "Could specify error handling for missing keys"
      ],
      "improved_prompt": "You are a senior Python engineer...",
      "explanation": "Excellent breakdown of requirements and expected data schema."
    }
  }
  ```

#### Get Latest Submission for Challenge
- **Endpoint**: `GET /api/submissions/latest?challengeId=1`
- **Access**: Authenticated

---

### 🃏 Glossary & User Progress

#### Get Prompting Glossary (Cached)
- **Endpoint**: `GET /api/glossary`
- **Access**: Public
- **Cache**: Cached in Redis and cached on the client via `Cache-Control: public, max-age=3600`

#### Get User Domain Progress
- **Endpoint**: `GET /api/user/progress`
- **Access**: Authenticated
- **Response**:
  ```json
  [
    {
      "id": 1,
      "domain": "Software Engineering",
      "currentLevel": 2,
      "xp": 175
    }
  ]
  ```

---

### ⚡ AI Evaluator Microservice (Direct)

| Method | Endpoint | Port | Description |
|---|---|:---:|---|
| `GET` | `/health` | `8081` | Microservice health check probe |
| `POST` | `/api/evaluate` | `8081` | Direct evaluation endpoint |

---

## 🗄️ Database Schema

```text
  +------------------+         +------------------+
  |      users       |         |    challenges    |
  +------------------+         +------------------+
  | id (PK)          |         | id (PK)          |
  | name             |         | domain           |
  | email (UQ)       |         | level            |
  | password         |         | hardness         |
  | role             |         | title            |
  | created_at       |         | task             |
  +--------+---------+         | topic_taught     |
           |                   | ai_eval_guide    |
           |                   | teaching_rule    |
           |                   | constraints      |
           |                   +--------+---------+
           | 1                          | 1
           |                            |
           |      +----------------+    |
           +----->|  submissions   |<---+
           |    N +----------------+  N
           |      | id (PK)        |
           |      | user_id (FK)   |
           |      | chal_id (FK)   |
           |      | user_prompt    |
           |      | score          |
           |      | feedback (JSON)|
           |      | created_at     |
           |      +----------------+
           |
           | 1    +----------------+
           +----->| user_progress  |
                N +----------------+
                  | id (PK)        |
                  | user_id (FK)   |
                  | domain         |
                  | current_level  |
                  | xp             |
                  | updated_at     |
                  +----------------+
```

---

## 🧪 Testing & Production

### Building Backend for Production
```bash
./mvnw clean package -DskipTests -f backend/pom.xml
java -jar backend/target/promptbetter-backend-0.0.1-SNAPSHOT.jar
```

### Compiling Rust Microservice for Release
```bash
cd ai-evaluator
cargo build --release
./target/release/ai-evaluator
```

---

## 🤝 Contributing

Contributions are welcome! To get started:

1. **Fork** the repository.
2. **Create a branch** for your feature:
   ```bash
   git checkout -b feature/my-new-feature
   ```
3. **Commit** your changes:
   ```bash
   git commit -m "Add new prompt evaluation dimension"
   ```
4. **Push** to the branch:
   ```bash
   git push origin feature/my-new-feature
   ```
5. **Open a Pull Request** describing your work.

---

## 📄 License

This project is open-source software licensed under the **[MIT License](LICENSE)**.

---

<p align="center">Built with ❤️ for aspiring AI engineers and prompt craftspeople.</p>
