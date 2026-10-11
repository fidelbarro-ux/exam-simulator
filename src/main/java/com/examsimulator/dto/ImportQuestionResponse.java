package com.examsimulator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportQuestionResponse {
    private boolean success;
    private String message;
    private int totalQuestions;
}
