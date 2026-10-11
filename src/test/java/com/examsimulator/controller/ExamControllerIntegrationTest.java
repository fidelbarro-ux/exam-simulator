package com.examsimulator.controller;

import com.examsimulator.dto.ImportQuestionRequest;
import com.examsimulator.dto.ExamSubmissionRequest;
import com.examsimulator.dto.SubmittedAnswer;
import com.examsimulator.service.QuestionImportService;
import com.examsimulator.repository.ExamQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExamControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuestionImportService importService;

    @Autowired
    private ExamQuestionRepository questionRepository;

    @BeforeEach
    void importSample() {
        importService.importQuestions("""
                No. 1) Which statement is correct?
                a. One
                b. Two
                c. Three
                d. Four
                Correct Answer: B
                Explanation: Two is the correct option for the first question.

                No. 2) Which statement is correct?
                a. One
                b. Two
                c. Three
                d. Four
                Correct Answer: C
                Explanation: Three is the correct option for the second question.
                """);
    }

    @Test
    void getQuestionsDoesNotExposeCorrectAnswer() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/exam/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].questionText").exists())
                .andExpect(jsonPath("$[0].options").isArray())
                .andExpect(jsonPath("$[0].correctAnswer").doesNotExist());
    }

    @Test
    void getQuestionsUsesTheRequestedExamLength() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/exam/questions").param("count", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void submitReturnsAnswerReviewAfterExamIsScored() throws Exception {
        var questions = questionRepository.findAllByOrderByQuestionNumberAsc();
        String json = """
                {
                  "answers": [
                    {"questionId": %d, "selectedAnswer": "A"},
                    {"questionId": %d, "selectedAnswer": "C"}
                  ]
                }
                """.formatted(questions.get(0).getId(), questions.get(1).getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/exam/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctAnswers").value(1))
                .andExpect(jsonPath("$.answerReviews").isArray())
                .andExpect(jsonPath("$.answerReviews[0].selectedAnswer").value("A"))
                .andExpect(jsonPath("$.answerReviews[0].correctAnswer").value("B"))
                .andExpect(jsonPath("$.answerReviews[0].correct").value(false))
                .andExpect(jsonPath("$.answerReviews[0].options").isArray())
                .andExpect(jsonPath("$.answerReviews[0].explanation")
                        .value("Two is the correct option for the first question."));
    }

    @Test
    void importEndpointWorks() throws Exception {
        String json = """
                {
                  "content": "1) Test question?\\na. One\\nb. Two\\nCorrect Answer: A\\nExplanation: One is correct."
                }
                """;

        mockMvc.perform(MockMvcRequestBuilders.post("/api/questions/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalQuestions").value(1));
    }
}
