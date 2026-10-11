package com.examsimulator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExamAnswerReviewResponse {
    private Integer questionNumber;
    private String questionText;
    private String selectedAnswer;
    private String selectedAnswerText;
    private String correctAnswer;
    private String correctAnswerText;
    private boolean correct;
    private String explanation;
    private List<ExamOptionResponse> options;
}
