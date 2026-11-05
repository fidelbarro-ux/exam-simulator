package com.examsimulator.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmittedAnswer {

    @NotNull(message = "Question ID is required")
    private Long questionId;

    private String selectedAnswer;
}
