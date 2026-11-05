package com.examsimulator.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ParsedExam {
    private final QuestionType type;
    private final List<ParsedQuestion> multipleChoiceQuestions;
    private final List<String> matchingAnswers;
    private final List<ParsedMatchingQuestion> matchingQuestions;
}
