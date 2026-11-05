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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

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
        var questions = questionRepository.findAllBySessionIdOrderByQuestionNumberAsc("default");
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

    @Test
    void matchingImportExposesTermsButNotCorrectAnswersAndSubmitsToResults() throws Exception {
        String importJson = """
                {"content":"TYPE: MATCHING\\nANSWERS:\\n- Static testing\\n- Unit testing\\nQUESTIONS:\\n1. Testing individual components in isolation.\\n2. Which activity identifies potential defects early?\\nCORRECT ANSWERS:\\n1. Unit testing\\n2. Static testing\\nEXPLANATIONS:\\n1. Unit testing checks individual components in isolation.\\n2. Static testing identifies defects without executing software."}
                """;
        mockMvc.perform(MockMvcRequestBuilders.post("/api/questions/import")
                        .contentType(MediaType.APPLICATION_JSON).content(importJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalQuestions").value(2));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/exam/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionType").value("MATCHING"))
                .andExpect(jsonPath("$[0].matchingGroup").exists())
                .andExpect(jsonPath("$[0].options[0].text").exists())
                .andExpect(jsonPath("$[0].correctAnswer").doesNotExist());

        var questions = questionRepository.findAllBySessionIdOrderByQuestionNumberAsc("default");
        String submit = """
                {"answers":[{"questionId":%d,"selectedAnswer":"TERM_2"},{"questionId":%d,"selectedAnswer":"TERM_1"}]}
                """.formatted(questions.get(0).getId(), questions.get(1).getId());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/exam/submit")
                        .contentType(MediaType.APPLICATION_JSON).content(submit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctAnswers").value(2))
                .andExpect(jsonPath("$.answerReviews[0].questionType").value("MATCHING"));
    }

    @Test
    void questionFormatTemplatesAreDownloadable() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/templates/matching-question-template.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TYPE: MATCHING")));
        mockMvc.perform(MockMvcRequestBuilders.get("/templates/multiple-choice-question-template.txt"))
                .andExpect(status().isOk());
    }

    @Test
    void questionBanksAreIsolatedByExamSession() throws Exception {
        String oneQuestion = """
                {"content":"1) First session question?\\na. One\\nb. Two\\nCorrect Answer: A\\nExplanation: One is correct."}
                """;
        String twoQuestions = """
                {"content":"1) Second session question?\\na. One\\nb. Two\\nCorrect Answer: A\\nExplanation: One is correct.\\n\\n2) Another question?\\na. One\\nb. Two\\nCorrect Answer: B\\nExplanation: Two is correct."}
                """;

        mockMvc.perform(MockMvcRequestBuilders.post("/api/questions/import")
                        .header("X-Exam-Session-Id", "tab-one")
                        .contentType(MediaType.APPLICATION_JSON).content(oneQuestion))
                .andExpect(status().isCreated());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/questions/import")
                        .header("X-Exam-Session-Id", "tab-two")
                        .contentType(MediaType.APPLICATION_JSON).content(twoQuestions))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/exam/questions")
                        .header("X-Exam-Session-Id", "tab-one"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/exam/questions")
                        .header("X-Exam-Session-Id", "tab-two"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
