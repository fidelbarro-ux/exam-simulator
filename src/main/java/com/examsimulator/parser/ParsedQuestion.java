package com.examsimulator.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedQuestion {
    private Integer questionNumber;
    private String questionText;
    private List<ParsedOption> options = new ArrayList<>();
    private String correctAnswer;
    private String explanation;
}
