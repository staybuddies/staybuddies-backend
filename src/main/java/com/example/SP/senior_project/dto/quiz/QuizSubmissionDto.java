package com.example.SP.senior_project.dto.quiz;


import lombok.Data;
import java.util.List;

@Data
public class QuizSubmissionDto {
    // exactly 10 integers (weights)
    private List<Integer> answers;
}
