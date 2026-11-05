package com.examsimulator.service;

import com.examsimulator.dto.ImportQuestionResponse;
import com.examsimulator.entity.ExamOption;
import com.examsimulator.entity.ExamQuestion;
import com.examsimulator.parser.*;
import com.examsimulator.repository.ExamQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionImportService {

    private final QuestionParserService parserService;
    private final ExamQuestionRepository questionRepository;

    @Transactional
    public ImportQuestionResponse importQuestions(String content) {
        return importQuestions(content, "default");
    }

    @Transactional
    public ImportQuestionResponse importQuestions(String content, String sessionId) {
        // Parse and validate everything before changing the existing question set.
        ParsedExam parsedExam = parserService.parseImport(content);

        questionRepository.deleteAll(questionRepository.findAllBySessionIdOrderByQuestionNumberAsc(sessionId));

        if (parsedExam.getType() == QuestionType.MATCHING) {
            importMatching(parsedExam, sessionId);
        } else {
            importMultipleChoice(parsedExam.getMultipleChoiceQuestions(), sessionId);
        }
        int total = parsedExam.getType() == QuestionType.MATCHING
                ? parsedExam.getMatchingQuestions().size()
                : parsedExam.getMultipleChoiceQuestions().size();
        return new ImportQuestionResponse(true, "Questions imported successfully", total);
    }

    private void importMultipleChoice(List<ParsedQuestion> parsedQuestions, String sessionId) {
        for (ParsedQuestion parsed : parsedQuestions) {
            ExamQuestion question = new ExamQuestion();
            question.setQuestionNumber(parsed.getQuestionNumber());
            question.setQuestionText(parsed.getQuestionText());
            question.setCorrectAnswer(parsed.getCorrectAnswer().toUpperCase());
            question.setExplanation(parsed.getExplanation());
            question.setCreatedAt(LocalDateTime.now());
            question.setQuestionType(QuestionType.MULTIPLE_CHOICE.name());
            question.setSessionId(sessionId);

            for (ParsedOption parsedOption : parsed.getOptions()) {
                ExamOption option = new ExamOption();
                option.setOptionLetter(parsedOption.getOptionLetter().toUpperCase());
                option.setOptionText(parsedOption.getOptionText());
                question.addOption(option);
            }

            questionRepository.save(question);
        }
    }

    private void importMatching(ParsedExam parsedExam, String sessionId) {
        String group = UUID.randomUUID().toString();
        for (ParsedMatchingQuestion parsed : parsedExam.getMatchingQuestions()) {
            ExamQuestion question = new ExamQuestion();
            question.setQuestionNumber(parsed.getQuestionNumber());
            question.setQuestionText(parsed.getQuestionText());
            question.setCorrectAnswer("TERM_" + (parsedExam.getMatchingAnswers().indexOf(parsed.getCorrectAnswer()) + 1));
            question.setExplanation(parsed.getExplanation());
            question.setCreatedAt(LocalDateTime.now());
            question.setQuestionType(QuestionType.MATCHING.name());
            question.setMatchingGroup(group);
            question.setSessionId(sessionId);
            for (int index = 0; index < parsedExam.getMatchingAnswers().size(); index++) {
                ExamOption option = new ExamOption();
                option.setOptionLetter("TERM_" + (index + 1));
                option.setOptionText(parsedExam.getMatchingAnswers().get(index));
                question.addOption(option);
            }
            questionRepository.save(question);
        }
    }
}
