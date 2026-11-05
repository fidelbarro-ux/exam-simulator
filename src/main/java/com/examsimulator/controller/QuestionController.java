package com.examsimulator.controller;

import com.examsimulator.dto.ImportQuestionRequest;
import com.examsimulator.dto.ImportQuestionResponse;
import com.examsimulator.service.QuestionImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionImportService questionImportService;

    @PostMapping("/import")
    public ResponseEntity<ImportQuestionResponse> importQuestions(
            @Valid @RequestBody ImportQuestionRequest request,
            @RequestHeader(value = "X-Exam-Session-Id", required = false) String sessionId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionImportService.importQuestions(request.getContent(), normalizeSessionId(sessionId)));
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return "default";
        if (!sessionId.matches("[A-Za-z0-9-]{1,64}")) throw new IllegalArgumentException("Invalid exam session ID");
        return sessionId;
    }
}
