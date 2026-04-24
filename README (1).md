# Quiz Leaderboard System — Bajaj Finserv Health | SRM Internship Assignment

## Problem Statement

Build a Java application that:
1. Polls a validator API **10 times** (poll index 0–9) with a **5-second delay** between each poll
2. Collects quiz score events across all polls
3. **Deduplicates** events using `(roundId + participant)` as a composite key
4. **Aggregates** total scores per participant
5. **Submits** the final leaderboard (sorted by totalScore descending) **exactly once**

---

## Key Challenge — Duplicate Event Handling

The API may return the same event data across multiple polls. Processing duplicates inflates scores and causes incorrect results.

**Wrong approach (without deduplication):**
```
Poll 0 → Alice R1 +10
Poll 3 → Alice R1 +10   ← same event again
Total Alice = 20 ❌
```

**Correct approach (with deduplication):**
```
Poll 0 → Alice R1 +10
Poll 3 → Alice R1 +10   ← DUPLICATE → ignored
Total Alice = 10 ✅
```

---

## Project Structure

```
quiz-leaderboard/
├── src/
│   ├── main/
│   │   ├── java/com/bajaj/quiz/
│   │   │   ├── QuizLeaderboardApplication.java   ← Spring Boot entry point
│   │   │   ├── config/
│   │   │   │   ├── AppConfig.java                ← RestTemplate bean
│   │   │   │   └── JacksonConfig.java            ← ObjectMapper bean
│   │   │   ├── model/
│   │   │   │   ├── QuizEvent.java                ← Single score event
│   │   │   │   ├── PollResponse.java             ← GET /quiz/messages response
│   │   │   │   ├── LeaderboardEntry.java         ← Leaderboard row
│   │   │   │   ├── SubmitRequest.java            ← POST /quiz/submit request
│   │   │   │   └── SubmitResponse.java           ← POST /quiz/submit response
│   │   │   └── service/
│   │   │       ├── QuizService.java              ← Core pipeline logic
│   │   │       └── QuizRunner.java               ← Auto-runs on startup
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/bajaj/quiz/
│           └── QuizLogicTest.java                ← Unit tests (no network)
└── pom.xml
```

---

## How It Works — Step by Step

| Step | Description |
|------|-------------|
| 1 | Poll `GET /quiz/messages?regNo=<REG_NO>&poll=0` to `poll=9` |
| 2 | Wait **5 seconds** between each poll (mandatory) |
| 3 | Deduplicate using `roundId + "::" + participant` as key |
| 4 | Aggregate scores: sum all unique scores per participant |
| 5 | Sort leaderboard by `totalScore` descending |
| 6 | Compute combined total score for verification |
| 7 | Submit once via `POST /quiz/submit` |

---

## Prerequisites

- Java 17+
- Maven 3.8+

---

## Setup & Run

### 1. Clone the repository

```bash
git clone https://github.com/your-username/quiz-leaderboard.git
cd quiz-leaderboard
```

### 2. Set your registration number

Edit `src/main/resources/application.properties`:

```properties
quiz.reg-no=YOUR_ACTUAL_REG_NO
```

Replace `YOUR_ACTUAL_REG_NO` with your actual registration number (e.g., `RA2311003010001`).

### 3. Build the project

```bash
mvn clean package -DskipTests
```

### 4. Run the application

```bash
java -jar target/quiz-leaderboard-1.0.0.jar
```

> **Note:** The full run takes approximately **45–50 seconds** due to the mandatory 5-second delay between polls.

---

## Expected Console Output

```
09:00:00 [INFO] QuizService - === Quiz Leaderboard System Starting ===
09:00:00 [INFO] QuizService - Registration Number : RA2311003010001
09:00:00 [INFO] QuizService - Polling index 0 of 9 ...
09:00:00 [INFO] QuizService -   Poll 0 -> 4 events received (setId=SET_1)
09:00:00 [INFO] QuizService -   Waiting 5000 ms before next poll...
...
09:00:45 [INFO] QuizService - Total raw events collected : 40
09:00:45 [INFO] QuizService - Duplicates ignored: 25
09:00:45 [INFO] QuizService - Unique events after deduplication : 15
09:00:45 [INFO] QuizService - === Leaderboard ===
09:00:45 [INFO] QuizService -   Bob -> 120
09:00:45 [INFO] QuizService -   Alice -> 100
09:00:45 [INFO] QuizService - Total combined score: 220
09:00:45 [INFO] QuizService - === Submission Result ===
09:00:45 [INFO] QuizService -   isCorrect      : true
09:00:45 [INFO] QuizService -   isIdempotent   : true
09:00:45 [INFO] QuizService -   submittedTotal : 220
09:00:45 [INFO] QuizService -   expectedTotal  : 220
09:00:45 [INFO] QuizService -   message        : Correct!
```

---

## Running Tests

```bash
mvn test
```

Tests cover:
- ✅ Basic deduplication (same roundId + participant is ignored)
- ✅ Score aggregation correctness
- ✅ Duplicates do NOT inflate scores
- ✅ Leaderboard sorted by totalScore descending
- ✅ Total score calculation
- ✅ Deduplication key format

---

## API Reference

### GET /quiz/messages

```
GET https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/messages?regNo=<REG_NO>&poll=<0-9>
```

**Response:**
```json
{
  "regNo": "RA2311003010001",
  "setId": "SET_1",
  "pollIndex": 0,
  "events": [
    { "roundId": "R1", "participant": "Alice", "score": 10 },
    { "roundId": "R1", "participant": "Bob", "score": 20 }
  ]
}
```

### POST /quiz/submit

```
POST https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/submit
```

**Request:**
```json
{
  "regNo": "RA2311003010001",
  "leaderboard": [
    { "participant": "Bob", "totalScore": 120 },
    { "participant": "Alice", "totalScore": 100 }
  ]
}
```

**Response:**
```json
{
  "isCorrect": true,
  "isIdempotent": true,
  "submittedTotal": 220,
  "expectedTotal": 220,
  "message": "Correct!"
}
```

---

## Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 17 | Core language |
| Spring Boot 3.2 | Application framework |
| Spring Web (RestTemplate) | HTTP client for API calls |
| Jackson | JSON serialization/deserialization |
| Lombok | Boilerplate reduction |
| JUnit 5 | Unit testing |
| Maven | Build tool |

---

## Author

- **Name:** [Your Name]
- **Reg No:** [Your Registration Number]
- **Assignment:** Bajaj Finserv Health | JAVA Qualifier | SRM | April 2025
