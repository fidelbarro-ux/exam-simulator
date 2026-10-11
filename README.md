# Exam Simulator

A complete, lightweight certification exam simulator built with:

- JDK 21
- Spring Boot 4.0.8
- Spring MVC REST
- Spring Data JPA / Hibernate
- H2 in-memory database
- Maven
- Lombok
- HTML5 / CSS3 / JavaScript / jQuery
- JUnit 5 / Mockito / MockMvc

Spring Boot 4.0.8 is used because it is a current 4.0.x maintenance release. The project intentionally uses the Spring Boot 4 `spring-boot-starter-webmvc` and H2 console modules.

## Run

Requirements:

- JDK 21
- Maven 3.6.3+

Run:

```bash
mvn spring-boot:run
```

Open:

```text
http://localhost:8080
```

H2 console:

```text
http://localhost:8080/h2-console
```

Connection settings:

```text
JDBC URL: jdbc:h2:mem:examsimulator
User: sa
Password: [empty]
```

## Application flow

1. Paste questions into the import screen.
2. Click **Generate Test**.
3. The backend parses and validates the complete question set transactionally.
4. The existing in-memory question set is replaced only after successful parsing/validation.
5. Click **Start Exam**.
6. The backend returns questions in a randomized order.
7. The browser uses checkbox controls but JavaScript enforces one selected answer per question.
8. Progress is updated as answers are selected.
9. Client-side validation prevents submission when questions are unanswered.
10. jQuery AJAX submits only question IDs and selected answer letters.
11. The backend performs the actual score calculation using the stored correct answers.
12. The result is displayed without reloading the page.
13. **Retry Exam** reloads the existing questions and randomizes them again.
14. **New Test** returns to the import screen.

## API

### POST /api/questions/import

Request:

```json
{
  "content": "exam questions..."
}
```

### GET /api/exam/questions

Returns randomized questions and options. Correct answers are intentionally omitted.

### POST /api/exam/submit

Request:

```json
{
  "answers": [
    {
      "questionId": 1,
      "selectedAnswer": "B"
    }
  ]
}
```

Response:

```json
{
  "totalQuestions": 20,
  "correctAnswers": 17,
  "incorrectAnswers": 3,
  "scorePercentage": 85.0
}
```

## Parser formats

Question numbering:

- `No. 1) Question`
- `No. 1. Question`
- `1) Question`
- `1. Question`

Options:

- `a. Answer`
- `A. Answer`
- `a) Answer`
- `A) Answer`

Correct answer:

- `Correct Answer: B`
- `Correct Answer: b`
- `Answer: B`
- `Answer: b`

Explanation (required):

- `Explanation: Why this option is correct`
- Explanation text may continue on following lines.

Option letters are normalized to uppercase.

## Security design

The browser-facing question response does not contain `correctAnswer`.

The browser stores only:

- question ID
- question number/text
- option letters/text
- selected answer

The backend retrieves correct answers from H2 and performs the final comparison.

## Testing

Run:

```bash
mvn test
```

The test suite covers parser formats and validation, scoring, case-insensitive answers, invalid question IDs, invalid answer letters, missing answers, duplicate submissions, and REST integration.

## Project structure

```text
src/main/java/com/examsimulator
├── ExamSimulatorApplication.java
├── controller
│   ├── QuestionController.java
│   └── ExamController.java
├── service
│   ├── QuestionImportService.java
│   ├── QuestionParserService.java
│   └── ExamService.java
├── repository
│   ├── ExamQuestionRepository.java
│   └── ExamOptionRepository.java
├── entity
│   ├── ExamQuestion.java
│   └── ExamOption.java
├── dto
│   ├── ImportQuestionRequest.java
│   ├── ImportQuestionResponse.java
│   ├── ExamQuestionResponse.java
│   ├── ExamOptionResponse.java
│   ├── ExamSubmissionRequest.java
│   ├── SubmittedAnswer.java
│   └── ExamResultResponse.java
├── parser
│   ├── ParsedQuestion.java
│   └── ParsedOption.java
└── exception
    ├── InvalidQuestionFormatException.java
    ├── ExamValidationException.java
    └── GlobalExceptionHandler.java

src/main/resources/static
├── index.html
├── exam.html
├── css/style.css
└── js
    ├── index.js
    └── exam.js
```
