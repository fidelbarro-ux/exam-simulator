package com.examsimulator.service;

import com.examsimulator.dto.ImportQuestionResponse;
import com.examsimulator.entity.ExamOption;
import com.examsimulator.entity.ExamQuestion;
import com.examsimulator.parser.ParsedOption;
import com.examsimulator.parser.ParsedQuestion;
import com.examsimulator.repository.ExamOptionRepository;
import com.examsimulator.repository.ExamQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionImportService {

    private final QuestionParserService parserService;
    private final ExamQuestionRepository questionRepository;
    private final ExamOptionRepository optionRepository;

    @Transactional
    public ImportQuestionResponse importQuestions(String content) {
        // Parse and validate everything before changing the existing question set.
        List<ParsedQuestion> parsedQuestions = parserService.parse(content);

        optionRepository.deleteAllInBatch();
        questionRepository.deleteAllInBatch();

        for (ParsedQuestion parsed : parsedQuestions) {
            ExamQuestion question = new ExamQuestion();
            question.setQuestionNumber(parsed.getQuestionNumber());
            question.setQuestionText(parsed.getQuestionText());
            question.setCorrectAnswer(parsed.getCorrectAnswer().toUpperCase());
            question.setExplanation(parsed.getExplanation());
            question.setCreatedAt(LocalDateTime.now());

            for (ParsedOption parsedOption : parsed.getOptions()) {
                ExamOption option = new ExamOption();
                option.setOptionLetter(parsedOption.getOptionLetter().toUpperCase());
                option.setOptionText(parsedOption.getOptionText());
                question.addOption(option);
            }

            questionRepository.save(question);
        }

        return new ImportQuestionResponse(
                true,
                "Questions imported successfully",
                parsedQuestions.size());
    }
}
