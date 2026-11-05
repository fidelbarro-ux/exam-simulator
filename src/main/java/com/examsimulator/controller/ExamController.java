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
            @RequestParam(required = false) Integer count,
            @RequestHeader(value = "X-Exam-Session-Id", required = false) String sessionId) {
        return examService.getRandomizedQuestions(count, normalizeSessionId(sessionId));
    }

    @PostMapping("/submit")
    public ExamResultResponse submit(@Valid @RequestBody ExamSubmissionRequest request,
                                     @RequestHeader(value = "X-Exam-Session-Id", required = false) String sessionId) {
        return examService.submit(request, normalizeSessionId(sessionId));
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return "default";
        if (!sessionId.matches("[A-Za-z0-9-]{1,64}")) throw new IllegalArgumentException("Invalid exam session ID");
        return sessionId;
    }
}
