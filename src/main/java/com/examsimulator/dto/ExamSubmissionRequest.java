package com.examsimulator.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ExamSubmissionRequest {

    @NotEmpty(message = "At least one answer must be submitted")
    @Valid
    private List<SubmittedAnswer> answers;
}
