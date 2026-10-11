package com.examsimulator.parser;

import com.examsimulator.exception.InvalidQuestionFormatException;
import com.examsimulator.service.QuestionParserService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionParserServiceTest {

    private final QuestionParserService parser = new QuestionParserService();

    @Test
    void parsesOneQuestion() {
        String content = """
                No. 1) Which statement is correct?
                a. First
                b. Second
                c. Third
                d. Fourth
                Correct Answer: B
                Explanation: Second is the correct answer.
                """;

        List<ParsedQuestion> result = parser.parse(content);

        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().getQuestionNumber());
        assertEquals("Which statement is correct?", result.getFirst().getQuestionText());
        assertEquals(4, result.getFirst().getOptions().size());
        assertEquals("B", result.getFirst().getCorrectAnswer());
        assertEquals("Second is the correct answer.", result.getFirst().getExplanation());
    }

    @Test
    void parsesMultipleQuestionFormats() {
        String content = """
                No. 1. First question?
                A. One
                B. Two
                Answer: a
                Explanation: One is correct.

                2) Second question?
                a) One
                b) Two
                Correct Answer: B
                Explanation: Two is correct.

                3. Third question?
                A. One
                B. Two
                Correct Answer: A
                Explanation: One is correct.
                """;

        List<ParsedQuestion> result = parser.parse(content);

        assertEquals(3, result.size());
        assertEquals("A", result.get(0).getCorrectAnswer());
        assertEquals("B", result.get(1).getCorrectAnswer());
        assertEquals("A", result.get(2).getCorrectAnswer());
        assertEquals("A", result.get(0).getOptions().getFirst().getOptionLetter());
    }

    @Test
    void parsesQuestionNoNumberingFormat() {
        String content = """
                Question No. 1) Which option is correct?
                A. One
                B. Two
                Correct Answer: A
                Explanation: One is correct.
                """;

        List<ParsedQuestion> result = parser.parse(content);

        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().getQuestionNumber());
        assertEquals("Which option is correct?", result.getFirst().getQuestionText());
    }

    @Test
    void supportsMultilineOptionText() {
        String content = """
                1) Which option is correct?
                A. This is a long option
                   that continues on the next line
                B. Another answer
                Correct Answer: A
                Explanation: The first option continues across two lines
                and is therefore correct.
                """;

        List<ParsedQuestion> result = parser.parse(content);

        assertEquals(
                "This is a long option that continues on the next line",
                result.getFirst().getOptions().getFirst().getOptionText());
        assertEquals(
                "The first option continues across two lines and is therefore correct.",
                result.getFirst().getExplanation());
    }

//    @Test
//    void rejectsInvalidCorrectAnswer() {
//        String content = """
//                1) Which option is correct?
//                A. One
//                B. Two
//                C. Three
//                D. Four
//                Correct Answer: E
//                """;
//
//        InvalidQuestionFormatException ex =
//                assertThrows(InvalidQuestionFormatException.class, () -> parser.parse(content));
//
//        System.out.println("EXCEPTION MESSAGE = [" + ex.getMessage() + "]");
//        assertTrue(ex.getMessage().contains("invalid correct answer"));
//    }

    @Test
    void rejectsMissingCorrectAnswer() {
        String content = """
                1) Which option is correct?
                A. One
                B. Two
                """;

        assertThrows(InvalidQuestionFormatException.class, () -> parser.parse(content));
    }

    @Test
    void rejectsMissingOptions() {
        String content = """
                1) Which option is correct?
                Correct Answer: A
                """;

        assertThrows(InvalidQuestionFormatException.class, () -> parser.parse(content));
    }

    @Test
    void rejectsMissingExplanation() {
        String content = """
                1) Which option is correct?
                A. One
                B. Two
                Correct Answer: A
                """;

        assertThrows(InvalidQuestionFormatException.class, () -> parser.parse(content));
    }

    @Test
    void rejectsDuplicateQuestionNumbers() {
        String content = """
                1) First?
                A. One
                B. Two
                Correct Answer: A
                Explanation: One is correct.

                1) Second?
                A. One
                B. Two
                Correct Answer: B
                Explanation: Two is correct.
                """;

        assertThrows(InvalidQuestionFormatException.class, () -> parser.parse(content));
    }
}
