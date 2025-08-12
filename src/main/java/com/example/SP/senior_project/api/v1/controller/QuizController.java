package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.quiz.QuizDto;
import com.example.SP.senior_project.dto.quiz.QuizSubmissionDto;
import com.example.SP.senior_project.service.QuizDualStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/room-finder/me/quiz")
public class QuizController {

    private final QuizDualStoreService quizService;

    @GetMapping
    public QuizDto get(@AuthenticationPrincipal UserDetails ud) {
        return quizService.getForUser(ud.getUsername());
    }

    @PostMapping
    public ResponseEntity<QuizDto> submit(@AuthenticationPrincipal UserDetails ud,
                                          @RequestBody QuizSubmissionDto req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quizService.saveForUser(ud.getUsername(), req));
    }

    @PutMapping
    public QuizDto update(@AuthenticationPrincipal UserDetails ud,
                          @RequestBody QuizSubmissionDto req) {
        return quizService.saveForUser(ud.getUsername(), req);
    }
}
