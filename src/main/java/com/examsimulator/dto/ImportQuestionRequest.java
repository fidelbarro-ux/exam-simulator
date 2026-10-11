package com.examsimulator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImportQuestionRequest {

    @NotBlank(message = "Import content must not be empty")
    private String content;
}
