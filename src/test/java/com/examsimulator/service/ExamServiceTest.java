package com.examsimulator.service;

import com.examsimulator.dto.ExamResultResponse;
import com.examsimulator.dto.ExamSubmissionRequest;
import com.examsimulator.dto.SubmittedAnswer;
import com.examsimulator.entity.ExamOption;
import com.examsimulator.entity.ExamQuestion;
import com.examsimulator.exception.ExamValidationException;
import com.examsimulator.repository.ExamQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    private ExamQuestionRepository repository;

    private ExamService service;
    private List<ExamQuestion> questions;

    @BeforeEach
    void setUp() {
        service = new ExamService(repository);
        questions = List.of(
                question(1L, 1, "A"),
                question(2L, 2, "B"),
                question(3L, 3, "C"),
                question(4L, 4, "D")
        );
        when(repository.findAllByOrderByQuestionNumberAsc()).thenReturn(questions);
    }

    @Test
    void scoresAllAnswersCorrect() {
        ExamResultResponse result = service.submit(submission("A", "B", "C", "D"));

        assertEquals(4, result.getTotalQuestions());
        assertEquals(4, result.getCorrectAnswers());
        assertEquals(0, result.getIncorrectAnswers());
        assertEquals(100.0, result.getScorePercentage());
    }

    @Test
    void scoresAllAnswersIncorrect() {
        ExamResultResponse result = service.submit(submission("B", "C", "D", "A"));

        assertEquals(0, result.getCorrectAnswers());
        assertEquals(4, result.getIncorrectAnswers());
        assertEquals(0.0, result.getScorePercentage());
    }

    @Test
    void scoresPartialResult() {
        ExamResultResponse result = service.submit(submission("A", "C", "C", "A"));

        assertEquals(2, result.getCorrectAnswers());
        assertEquals(2, result.getIncorrectAnswers());
        assertEquals(50.0, result.getScorePercentage());
    }

    @Test
    void comparesAnswersCaseInsensitively() {
        ExamResultResponse result = service.submit(submission("a", "b", "c", "d"));

        assertEquals(4, result.getCorrectAnswers());
    }

    @Test
    void returnsTheRequestedNumberOfRandomizedQuestions() {
        var result = service.getRandomizedQuestions(2);

        assertEquals(2, result.size());
        assertEquals(2, result.stream().map(question -> question.getId()).distinct().count());
    }

    @Test
    void rejectsAQuestionCountAboveTheAvailableQuestions() {
        assertThrows(ExamValidationException.class, () -> service.getRandomizedQuestions(5));
    }

    @Test
    void includesAnswerReviewWithSelectedCorrectAnswerAndExplanation() {
        ExamResultResponse result = service.submit(submission("B", "B", "C", "D"));

        assertEquals(4, result.getAnswerReviews().size());

        var firstReview = result.getAnswerReviews().getFirst();
        assertEquals(1, firstReview.getQuestionNumber());
        assertEquals("B", firstReview.getSelectedAnswer());
        assertEquals("Option B", firstReview.getSelectedAnswerText());
        assertEquals("A", firstReview.getCorrectAnswer());
        assertEquals("Option A", firstReview.getCorrectAnswerText());
        assertFalse(firstReview.isCorrect());
        assertEquals("Explanation for question 1", firstReview.getExplanation());

        var secondReview = result.getAnswerReviews().get(1);
        assertTrue(secondReview.isCorrect());
    }

    @Test
    void rejectsUnknownQuestionId() {
        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(
                answer(1L, "A"),
                answer(2L, "B"),
                answer(3L, "C"),
                answer(999L, "D")
        ));

        assertThrows(ExamValidationException.class, () -> service.submit(request));
    }

    @Test
    void rejectsInvalidAnswerLetter() {
        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(
                answer(1L, "A"),
                answer(2L, "B"),
                answer(3L, "C"),
                answer(4L, "Z")
        ));

        assertThrows(ExamValidationException.class, () -> service.submit(request));
    }

    @Test
    void scoresOnlyTheQuestionsIncludedInTheExam() {
        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answer(1L, "A"), answer(2L, "B")));

        ExamResultResponse result = service.submit(request);

        assertEquals(2, result.getTotalQuestions());
        assertEquals(2, result.getCorrectAnswers());
    }

    @Test
    void keepsTheExamQuestionOrderInTheAnswerReview() {
        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(answer(3L, "C"), answer(1L, "A"), answer(4L, "D")));

        ExamResultResponse result = service.submit(request);

        assertEquals(List.of(3, 1, 4), result.getAnswerReviews().stream()
                .map(review -> review.getQuestionNumber())
                .toList());
    }

    @Test
    void rejectsDuplicateQuestionId() {
        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(
                answer(1L, "A"),
                answer(1L, "B"),
                answer(2L, "B"),
                answer(3L, "C"),
                answer(4L, "D")
        ));

        assertThrows(ExamValidationException.class, () -> service.submit(request));
    }

    private ExamQuestion question(Long id, int number, String correct) {
        ExamQuestion question = new ExamQuestion();
        question.setId(id);
        question.setQuestionNumber(number);
        question.setQuestionText("Question " + number);
        question.setCorrectAnswer(correct);
        question.setExplanation("Explanation for question " + number);
        question.setCreatedAt(LocalDateTime.now());

        for (String letter : List.of("A", "B", "C", "D")) {
            ExamOption option = new ExamOption();
            option.setOptionLetter(letter);
            option.setOptionText("Option " + letter);
            question.addOption(option);
        }
        return question;
    }

    private ExamSubmissionRequest submission(String... answers) {
        ExamSubmissionRequest request = new ExamSubmissionRequest();
        request.setAnswers(List.of(
                answer(1L, answers[0]),
                answer(2L, answers[1]),
                answer(3L, answers[2]),
                answer(4L, answers[3])
        ));
        return request;
    }

    private SubmittedAnswer answer(Long id, String selected) {
        SubmittedAnswer answer = new SubmittedAnswer();
        answer.setQuestionId(id);
        answer.setSelectedAnswer(selected);
        return answer;
    }
}
