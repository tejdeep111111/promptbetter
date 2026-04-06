# PromptBetter 🚀

An AI-powered platform for practicing and improving prompt engineering skills. Users can take on challenges across various domains, submit their prompts, and receive AI-generated feedback to enhance their prompt crafting abilities.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [API Documentation](#-api-documentation)
- [Database Schema](#-database-schema)
- [Running the Application](#-running-the-application)
- [Contributing](#-contributing)

## ✨ Features

- **🎯 Domain-Based Challenges**: Practice prompt engineering across multiple domains (Software Engineering, Content Writing, Data Analysis, etc.)
- **📊 AI-Powered Evaluation**: Get instant feedback on your prompts with detailed scoring across multiple dimensions
- **🏆 Progress Tracking**: Track your XP and level progression in each domain
- **🔐 Secure Authentication**: JWT-based authentication system
- **⚡ Rate Limiting**: Built-in rate limiting to prevent API abuse
- **📈 Performance Monitoring**: Spring Actuator integration for health checks and metrics
- **💾 Caching**: Caffeine-based caching for improved performance

## 🛠 Tech Stack

### Backend
| Technology | Purpose |
|------------|---------|
| Java 17 | Programming Language |
| Spring Boot 3.2.0 | Application Framework |
| Spring Security | Authentication & Authorization |
| Spring Data JPA | Database ORM |
| MySQL | Relational Database |
| JWT (jjwt) | Token-based Authentication |
| Lombok | Boilerplate Code Reduction |
| Caffeine | In-memory Caching |
| Groq API (LLaMA 3.1) | AI Prompt Evaluation |

### Frontend
| Technology | Purpose |
|------------|---------|
| HTML/CSS/JavaScript | User Interface |
| Static Serving (npx serve) | Development Server |

## 📁 Project Structure

```
promptbetter/
├── backend/
│   ├── pom.xml                          # Maven configuration
│   └── src/
│       ├── main/
│       │   ├── java/com/promptbetter/
│       │   │   ├── PromptBetterApplication.java  # Main application class
│       │   │   ├── config/
│       │   │   │   ├── CacheConfig.java          # Caffeine cache configuration
│       │   │   │   ├── CorsConfig.java           # CORS settings
│       │   │   │   └── SecurityConfig.java       # Spring Security configuration
│       │   │   ├── controller/
│       │   │   │   ├── AuthController.java       # Authentication endpoints
│       │   │   │   ├── ChallengeController.java  # Challenge management endpoints
│       │   │   │   └── SubmissionController.java # Submission handling endpoints
│       │   │   ├── filters/
│       │   │   │   └── JwtAuthFilter.java        # JWT authentication filter
│       │   │   ├── model/
│       │   │   │   ├── Challenge.java            # Challenge entity
│       │   │   │   ├── Submission.java           # Submission entity
│       │   │   │   ├── User.java                 # User entity
│       │   │   │   └── UserProgress.java         # User progress entity
│       │   │   ├── repository/
│       │   │   │   ├── ChallengeRepository.java
│       │   │   │   ├── SubmissionRepository.java
│       │   │   │   ├── UserProgressRepository.java
│       │   │   │   └── UserRepository.java
│       │   │   ├── service/
│       │   │   │   ├── AuthService.java          # Authentication logic
│       │   │   │   ├── ChallengeService.java     # Challenge business logic
│       │   │   │   ├── PromptEvaluatorService.java # AI evaluation service
│       │   │   │   ├── RateLimiterService.java   # Rate limiting logic
│       │   │   │   ├── SubmissionService.java    # Submission processing
│       │   │   │   └── UserDetailsServiceImpl.java
│       │   │   └── util/
│       │   │       └── JwtUtil.java              # JWT utility methods
│       │   └── resources/
│       │       ├── application.properties        # Application configuration
│       │       └── script.sql                    # Database schema reference
│       └── test/                                 # Unit and integration tests
└── frontend/
    ├── promptbetter.html                         # Main frontend file
    └── README.txt                                # Frontend instructions
```

## 📋 Prerequisites

Before running this application, ensure you have the following installed:

- **Java 17** or higher
- **Maven 3.6+**
- **MySQL 8.0+**
- **Node.js** (for frontend serving)
- **Groq API Key** (for AI evaluation)

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/promptbetter.git
cd promptbetter
```

### 2. Set Up MySQL Database

```sql
CREATE DATABASE promptbetter;
```

### 3. Configure Environment Variables

Set the following environment variables:

| Variable | Description |
|----------|-------------|
| `MYSQL_PASSWORD` | Your MySQL root password |
| `API_KEY` | Your Groq API key |
| `PROMPTBETTER_JWT_SECRET` | A secure secret key for JWT signing |

**Windows (PowerShell):**
```powershell
$env:MYSQL_PASSWORD = "your_mysql_password"
$env:API_KEY = "your_groq_api_key"
$env:PROMPTBETTER_JWT_SECRET = "your_jwt_secret_key"
```

**Linux/macOS:**
```bash
export MYSQL_PASSWORD="your_mysql_password"
export API_KEY="your_groq_api_key"
export PROMPTBETTER_JWT_SECRET="your_jwt_secret_key"
```

### 4. Build and Run Backend

```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

**On Windows:**
```cmd
cd backend
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

### 5. Run Frontend

```bash
cd frontend
npx serve .
```

## ⚙️ Configuration

The application configuration is located in `backend/src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/promptbetter
spring.datasource.username=root
spring.datasource.password=${MYSQL_PASSWORD}

# AI Configuration
api.key=${API_KEY}
api.base-url=https://api.groq.com/openai/v1/chat/completions
ai.model=llama-3.1-8b-instant

# JWT Configuration
jwt.secret=${PROMPTBETTER_JWT_SECRET}
```

## 📚 API Documentation

### Authentication Endpoints

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "securepassword"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "securepassword"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Challenge Endpoints

#### Get All Domains
```http
GET /api/challenges/domains
Authorization: Bearer <token>
```

#### Get Challenges by Domain
```http
GET /api/challenges?domain=Software%20Engineering
Authorization: Bearer <token>
```

#### Get Current Challenge
```http
GET /api/challenges/current?domain=Software%20Engineering&level=1
Authorization: Bearer <token>
```

### Submission Endpoints

#### Submit Prompt
```http
POST /api/submissions
Authorization: Bearer <token>
Content-Type: application/json

{
  "challengeId": 1,
  "userPrompt": "Your prompt here..."
}
```

**Response:**
```json
{
  "score": 85,
  "feedback": {
    "score": 85,
    "strengths": ["Clear instructions", "Good context"],
    "flaws": ["Could be more specific"],
    "improved_prompt": "...",
    "explanation": "...",
    "dimensions": {
      "clarity": 18,
      "context": 17,
      "specificity": 15,
      "constraints": 18,
      "technique": 17
    }
  },
  "leveledUp": true,
  "nextLevel": 2
}
```

#### Get Latest Submission
```http
GET /api/submissions/latest?challengeId=1
Authorization: Bearer <token>
```

### Actuator Endpoints (Monitoring)

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Application health status |
| `/actuator/info` | Application information |
| `/actuator/metrics` | Application metrics |
| `/actuator/caches` | Cache statistics |

## 🗄️ Database Schema

### Users Table
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| name | VARCHAR(100) | User's display name |
| email | VARCHAR(150) | Unique email address |
| password | VARCHAR(255) | Hashed password |
| created_at | TIMESTAMP | Account creation time |

### Challenges Table
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| domain | VARCHAR(100) | Challenge domain |
| level | INT | Difficulty level |
| hardness | VARCHAR(50) | Difficulty label |
| title | VARCHAR(200) | Challenge title |
| task | TEXT | Challenge description |
| topic_taught | VARCHAR(100) | Learning topic |
| ai_evaluation_guide | TEXT | AI evaluation criteria |
| constraint_as_string | VARCHAR(255) | Challenge constraints |
| key_takeaway | TEXT | Learning takeaway |

### Submissions Table
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| user_id | BIGINT | Foreign key to users |
| challenge_id | BIGINT | Foreign key to challenges |
| user_prompt | TEXT | Submitted prompt |
| score | INT | Evaluation score (0-100) |
| feedback | JSON | Detailed AI feedback |
| created_at | TIMESTAMP | Submission time |

### User Progress Table
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| user_id | BIGINT | Foreign key to users |
| domain | VARCHAR(100) | Domain name |
| current_level | INT | Current level in domain |
| xp | INT | Experience points |
| updated_at | TIMESTAMP | Last update time |

## 🏃 Running the Application

### Development Mode

1. **Start the backend:**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

2. **Start the frontend:**
   ```bash
   cd frontend
   npx serve .
   ```

3. **Access the application:**
   - Frontend: http://localhost:3000 (or the port shown by serve)
   - Backend API: http://localhost:8080
   - Health Check: http://localhost:8080/actuator/health

### Production Build

```bash
cd backend
./mvnw clean package -DskipTests
java -jar target/promptbetter-backend-0.0.1-SNAPSHOT.jar
```

## 🧪 Testing

Run the test suite:

```bash
cd backend
./mvnw test
```

## 📊 Evaluation Dimensions

Prompts are evaluated across 5 dimensions (each scored 0-20):

| Dimension | Description |
|-----------|-------------|
| **Clarity** | How clear and understandable the prompt is |
| **Context** | How well context is provided to the AI |
| **Specificity** | How specific and detailed the instructions are |
| **Constraints** | How well boundaries and limitations are defined |
| **Technique** | Use of prompt engineering techniques |

**Scoring:**
- Score ≥ 70: User levels up to the next challenge
- XP is accumulated based on scores

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot) - Backend framework
- [Groq](https://groq.com/) - AI API provider
- [LLaMA](https://ai.meta.com/llama/) - AI model

---

<p align="center">Made with ❤️ for better prompt engineering</p>

