package com.example.SP.senior_project.dto.quiz;

import lombok.Data;

import java.util.List;

@Data
public class QuizDto {
    private List<Integer> answers;
    private Integer totalScore;
}
