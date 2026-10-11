package com.examsimulator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExamResultResponse {
    private int totalQuestions;
    private int correctAnswers;
    private int incorrectAnswers;
    private double scorePercentage;
    private List<ExamAnswerReviewResponse> answerReviews;
}
