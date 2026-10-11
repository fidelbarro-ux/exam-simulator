package com.examsimulator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExamOptionResponse {
    private String letter;
    private String text;
}
