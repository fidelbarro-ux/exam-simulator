package com.examsimulator.service;

import com.examsimulator.exception.InvalidQuestionFormatException;
import com.examsimulator.parser.ParsedOption;
import com.examsimulator.parser.ParsedQuestion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionParserService {

    private static final Pattern QUESTION_PATTERN =
            Pattern.compile("^\\s*(?:(?:Question\\s+)?No\\.\\s*)?(\\d+)\\s*[.)]\\s*(.*)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern OPTION_PATTERN =
            Pattern.compile("^\\s*([A-Da-d])[.)]\\s+(.*)$");

    private static final Pattern ANSWER_PATTERN =
            Pattern.compile("^\\s*(?:Correct\\s+Answer|Answer)\\s*:\\s*([A-Da-d])\\s*$",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern EXPLANATION_PATTERN =
            Pattern.compile("^\\s*Explanation\\s*:\\s*(.*)$", Pattern.CASE_INSENSITIVE);

    public List<ParsedQuestion> parse(String content) {
        if (content == null || content.isBlank()) {
            throw new InvalidQuestionFormatException("Import content must not be empty");
        }

        List<ParsedQuestion> questions = new ArrayList<>();
        ParsedQuestion current = null;
        ParsedOption currentOption = null;
        boolean answerSeenForCurrent = false;
        boolean explanationSeenForCurrent = false;

        String[] lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n");

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.isEmpty()) {
                continue;
            }

            Matcher questionMatcher = QUESTION_PATTERN.matcher(line);
            if (questionMatcher.matches()) {
                if (current != null) {
                    validateQuestion(current, answerSeenForCurrent, explanationSeenForCurrent);
                    questions.add(current);
                }

                int number;
                try {
                    number = Integer.parseInt(questionMatcher.group(1));
                } catch (NumberFormatException ex) {
                    throw new InvalidQuestionFormatException("Invalid question number: " + questionMatcher.group(1));
                }

                String text = questionMatcher.group(2).trim();
                current = new ParsedQuestion(number, text, new ArrayList<>(), null, null);
                currentOption = null;
                answerSeenForCurrent = false;
                explanationSeenForCurrent = false;
                continue;
            }

            if (current == null) {
                throw new InvalidQuestionFormatException(
                        "Malformed question format. Expected a question such as 'No. 1) Question text'");
            }

            Matcher optionMatcher = OPTION_PATTERN.matcher(line);
            if (optionMatcher.matches()) {
                String letter = optionMatcher.group(1).toUpperCase();
                String optionText = optionMatcher.group(2).trim();
                currentOption = new ParsedOption(letter, optionText);
                current.getOptions().add(currentOption);
                continue;
            }

            Matcher answerMatcher = ANSWER_PATTERN.matcher(line);
            if (answerMatcher.matches()) {
                String answer = answerMatcher.group(1).toUpperCase();
                if (answerSeenForCurrent) {
                    throw new InvalidQuestionFormatException(
                            "Question " + current.getQuestionNumber() + " contains a duplicate correct answer declaration");
                }
                current.setCorrectAnswer(answer);
                answerSeenForCurrent = true;
                currentOption = null;
                continue;
            }

            Matcher explanationMatcher = EXPLANATION_PATTERN.matcher(line);
            if (explanationMatcher.matches()) {
                if (explanationSeenForCurrent) {
                    throw new InvalidQuestionFormatException(
                            "Question " + current.getQuestionNumber() + " contains a duplicate explanation declaration");
                }
                String explanation = explanationMatcher.group(1).trim();
                current.setExplanation(explanation);
                explanationSeenForCurrent = true;
                currentOption = null;
                continue;
            }

            if (currentOption != null) {
                currentOption.setOptionText(
                        (currentOption.getOptionText() + " " + line).trim());
            } else if (explanationSeenForCurrent) {
                current.setExplanation((current.getExplanation() + " " + line).trim());
            } else if (current.getOptions().isEmpty()) {
                current.setQuestionText(
                        (current.getQuestionText() + " " + line).trim());
            } else {
                throw new InvalidQuestionFormatException(
                        "Question " + current.getQuestionNumber() +
                                " contains text that is not part of an option or correct-answer declaration");
            }
        }

        if (current != null) {
            validateQuestion(current, answerSeenForCurrent, explanationSeenForCurrent);
            questions.add(current);
        }

        if (questions.isEmpty()) {
            throw new InvalidQuestionFormatException("No questions were found in the supplied content");
        }

        validateQuestionNumbers(questions);
        return questions;
    }

    private void validateQuestion(ParsedQuestion question, boolean answerSeen, boolean explanationSeen) {
        if (question.getQuestionNumber() == null) {
            throw new InvalidQuestionFormatException("Missing question number");
        }

        if (question.getQuestionText() == null || question.getQuestionText().isBlank()) {
            throw new InvalidQuestionFormatException(
                    "Question " + question.getQuestionNumber() + " is missing question text");
        }

        if (question.getOptions() == null || question.getOptions().size() < 2) {
            throw new InvalidQuestionFormatException(
                    "Question " + question.getQuestionNumber() + " is missing options");
        }

        Set<String> letters = new HashSet<>();
        for (ParsedOption option : question.getOptions()) {
            if (!letters.add(option.getOptionLetter())) {
                throw new InvalidQuestionFormatException(
                        "Question " + question.getQuestionNumber() +
                                " contains duplicate option letter: " + option.getOptionLetter());
            }
            if (option.getOptionText() == null || option.getOptionText().isBlank()) {
                throw new InvalidQuestionFormatException(
                        "Question " + question.getQuestionNumber() +
                                " has an empty option: " + option.getOptionLetter());
            }
        }

        if (!answerSeen || question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
            throw new InvalidQuestionFormatException(
                    "Question " + question.getQuestionNumber() + " is missing correct answer");
        }

        if (!letters.contains(question.getCorrectAnswer().toUpperCase())) {
            throw new InvalidQuestionFormatException(
                    "Question " + question.getQuestionNumber() +
                            " has an invalid correct answer: " + question.getCorrectAnswer());
        }

        if (!explanationSeen || question.getExplanation() == null || question.getExplanation().isBlank()) {
            throw new InvalidQuestionFormatException(
                    "Question " + question.getQuestionNumber() + " is missing explanation");
        }
    }

    private void validateQuestionNumbers(List<ParsedQuestion> questions) {
        Set<Integer> numbers = new HashSet<>();
        for (ParsedQuestion question : questions) {
            if (!numbers.add(question.getQuestionNumber())) {
                throw new InvalidQuestionFormatException(
                        "Duplicate question number: " + question.getQuestionNumber());
            }
        }
    }
}
