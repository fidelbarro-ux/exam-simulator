package com.examsimulator.service;

import com.examsimulator.dto.ExamOptionResponse;
import com.examsimulator.dto.ExamAnswerReviewResponse;
import com.examsimulator.dto.ExamQuestionResponse;
import com.examsimulator.dto.ExamResultResponse;
import com.examsimulator.dto.ExamSubmissionRequest;
import com.examsimulator.dto.SubmittedAnswer;
import com.examsimulator.entity.ExamQuestion;
import com.examsimulator.entity.ExamOption;
import com.examsimulator.exception.ExamValidationException;
import com.examsimulator.repository.ExamQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamQuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public List<ExamQuestionResponse> getRandomizedQuestions(Integer count) {
        List<ExamQuestion> questions = new ArrayList<>(questionRepository.findAllByOrderByQuestionNumberAsc());

        if (count != null && count < 1) {
            throw new ExamValidationException("Question count must be at least 1.");
        }
        if (count != null && count > questions.size()) {
            throw new ExamValidationException(
                    "Only " + questions.size() + " question" + (questions.size() == 1 ? " is" : "s are")
                            + " available.");
        }

        Collections.shuffle(questions);

        return questions.stream()
                .limit(count == null ? questions.size() : count)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExamResultResponse submit(ExamSubmissionRequest request) {
        List<ExamQuestion> allQuestions = questionRepository.findAllByOrderByQuestionNumberAsc();

        if (allQuestions.isEmpty()) {
            throw new ExamValidationException("No exam questions are available. Import questions first.");
        }

        validateSubmission(request, allQuestions);

        Map<Long, SubmittedAnswer> submittedByQuestionId = request.getAnswers().stream()
                .collect(Collectors.toMap(
                        SubmittedAnswer::getQuestionId,
                        answer -> answer));

        Map<Long, ExamQuestion> questionsById = allQuestions.stream()
                .collect(Collectors.toMap(ExamQuestion::getId, question -> question));
        List<ExamQuestion> questions = request.getAnswers().stream()
                .map(answer -> questionsById.get(answer.getQuestionId()))
                .toList();

        int correct = 0;
        List<ExamAnswerReviewResponse> answerReviews = new ArrayList<>();

        for (ExamQuestion question : questions) {
            SubmittedAnswer submitted = submittedByQuestionId.get(question.getId());
            String selected = submitted.getSelectedAnswer().trim().toUpperCase(Locale.ROOT);
            String correctAnswer = question.getCorrectAnswer().toUpperCase(Locale.ROOT);
            boolean isCorrect = correctAnswer.equalsIgnoreCase(selected);
            if (isCorrect) {
                correct++;
            }

            String selectedAnswerText = optionText(question, selected);
            String correctAnswerText = optionText(question, correctAnswer);
            answerReviews.add(new ExamAnswerReviewResponse(
                    question.getQuestionNumber(),
                    question.getQuestionText(),
                    selected,
                    selectedAnswerText,
                    correctAnswer,
                    correctAnswerText,
                    isCorrect,
                    question.getExplanation(),
                    question.getOptions().stream()
                            .map(option -> new ExamOptionResponse(option.getOptionLetter(), option.getOptionText()))
                            .toList()));
        }

        int total = questions.size();
        int incorrect = total - correct;
        double percentage = total == 0 ? 0.0 : Math.round((correct * 10000.0 / total)) / 100.0;

        return new ExamResultResponse(total, correct, incorrect, percentage, answerReviews);
    }

    private void validateSubmission(ExamSubmissionRequest request, List<ExamQuestion> questions) {
        if (request == null || request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new ExamValidationException("No answers were submitted");
        }

        Set<Long> knownQuestionIds = questions.stream()
                .map(ExamQuestion::getId)
                .collect(Collectors.toSet());

        Set<Long> submittedQuestionIds = new HashSet<>();

        for (SubmittedAnswer answer : request.getAnswers()) {
            if (answer.getQuestionId() == null) {
                throw new ExamValidationException("A submitted answer is missing its question ID");
            }

            if (!knownQuestionIds.contains(answer.getQuestionId())) {
                throw new ExamValidationException(
                        "Invalid question ID during exam submission: " + answer.getQuestionId());
            }

            if (!submittedQuestionIds.add(answer.getQuestionId())) {
                throw new ExamValidationException(
                        "Duplicate submitted answer for question ID: " + answer.getQuestionId());
            }

            if (answer.getSelectedAnswer() == null || answer.getSelectedAnswer().isBlank()) {
                throw new ExamValidationException(
                        "Missing answer for question ID: " + answer.getQuestionId());
            }

            String selected = answer.getSelectedAnswer().trim().toUpperCase(Locale.ROOT);
            ExamQuestion question = questions.stream()
                    .filter(q -> q.getId().equals(answer.getQuestionId()))
                    .findFirst()
                    .orElseThrow();

            boolean validOption = question.getOptions().stream()
                    .anyMatch(option -> option.getOptionLetter().equalsIgnoreCase(selected));

            if (!validOption) {
                throw new ExamValidationException(
                        "Invalid answer '" + answer.getSelectedAnswer() +
                                "' for question ID: " + answer.getQuestionId());
            }
        }

    }

    private ExamQuestionResponse toResponse(ExamQuestion question) {
        List<ExamOptionResponse> options = question.getOptions().stream()
                .map(option -> new ExamOptionResponse(option.getOptionLetter(), option.getOptionText()))
                .toList();

        // Intentionally omit correctAnswer from the browser-facing DTO.
        return new ExamQuestionResponse(
                question.getId(),
                question.getQuestionNumber(),
                question.getQuestionText(),
                options);
    }

    private String optionText(ExamQuestion question, String letter) {
        return question.getOptions().stream()
                .filter(option -> option.getOptionLetter().equalsIgnoreCase(letter))
                .map(ExamOption::getOptionText)
                .findFirst()
                .orElseThrow();
    }
}
