package com.examsimulator.parser;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ParsedMatchingQuestion {
    private Integer questionNumber;
    private String questionText;
    private String correctAnswer;
    private String explanation;
}
