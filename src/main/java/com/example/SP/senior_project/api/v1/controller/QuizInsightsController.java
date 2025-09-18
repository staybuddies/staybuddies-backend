package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.quiz.QuizInsightsDto;
import com.example.SP.senior_project.service.QuizInsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/quiz")
public class QuizInsightsController {

    private final QuizInsightsService svc;

    @GetMapping("/insights")
    public QuizInsightsDto myInsights(@AuthenticationPrincipal UserDetails ud) {
        return svc.insightsFor(ud.getUsername());
    }
}
