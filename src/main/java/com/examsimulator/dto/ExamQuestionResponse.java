package com.examsimulator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExamQuestionResponse {
    private Long id;
    private Integer questionNumber;
    private String questionText;
    private List<ExamOptionResponse> options;
}
