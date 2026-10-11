package com.examsimulator.controller;

import com.examsimulator.dto.ExamQuestionResponse;
import com.examsimulator.dto.ExamResultResponse;
import com.examsimulator.dto.ExamSubmissionRequest;
import com.examsimulator.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @GetMapping("/questions")
    public List<ExamQuestionResponse> getQuestions(
            @RequestParam(required = false) Integer count) {
        return examService.getRandomizedQuestions(count);
    }

    @PostMapping("/submit")
    public ExamResultResponse submit(@Valid @RequestBody ExamSubmissionRequest request) {
        return examService.submit(request);
    }
}
