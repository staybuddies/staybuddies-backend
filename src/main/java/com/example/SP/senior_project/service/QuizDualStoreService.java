// src/main/java/com/example/SP/senior_project/service/QuizDualStoreService.java
package com.example.SP.senior_project.service;

import com.example.SP.senior_project.dto.quiz.QuizDto;
import com.example.SP.senior_project.dto.quiz.QuizSubmissionDto;
import com.example.SP.senior_project.model.QuizResponse;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.model.RoomFinderQuiz;
import com.example.SP.senior_project.repository.QuizResponseRepository;
import com.example.SP.senior_project.repository.RoomFinderQuizRepository;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizDualStoreService {

    private final RoomFinderRepository roomFinderRepo;
    private final QuizResponseRepository responseRepo;
    private final RoomFinderQuizRepository quizRepo;

    /** Read: prefer QuizResponse (list) and fallback to RoomFinderQuiz (q1..q10). */
    public QuizDto getForUser(String email) {
        return responseRepo.findByRoomFinderEmail(email)
                .map(this::toDto)
                .or(() -> quizRepo.findByRoomFinderEmail(email).map(this::toDto))
                .orElseGet(() -> {
                    var dto = new QuizDto();
                    dto.setAnswers(List.of());
                    dto.setTotalScore(0);
                    return dto;
                });
    }

    /** Write: validate once, then upsert BOTH stores atomically. */
    @Transactional
    public QuizDto saveForUser(String email, QuizSubmissionDto req) {
        var answers = validateAnswers(req);

        RoomFinder user = roomFinderRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // 1) Upsert canonical list store
        QuizResponse resp = responseRepo.findByRoomFinderEmail(email)
                .orElseGet(() -> {
                    var r = new QuizResponse();
                    r.setRoomFinder(user);
                    return r;
                });
        // copy to avoid persistent list surprises
        resp.setAnswers(new ArrayList<>(answers));
        responseRepo.save(resp);

        // 2) Upsert denormalized cache with total score
        RoomFinderQuiz quiz = quizRepo.findByRoomFinderEmail(email)
                .orElseGet(() -> {
                    var q = new RoomFinderQuiz();
                    q.setRoomFinder(user);
                    return q;
                });
        setQ1toQ10(quiz, answers);
        quiz.setTotalScore(answers.stream().mapToInt(Integer::intValue).sum());
        quizRepo.save(quiz);

        // return the unified DTO
        var dto = new QuizDto();
        dto.setAnswers(answers);
        dto.setTotalScore(quiz.getTotalScore());
        return dto;
    }

    // ----- helpers -----

    private List<Integer> validateAnswers(QuizSubmissionDto req) {
        if (req.getAnswers() == null || req.getAnswers().size() != 10) {
            throw new IllegalArgumentException("Exactly 10 answers are required.");
        }
        for (Integer v : req.getAnswers()) {
            if (v == null || v < 1 || v > 5) {
                throw new IllegalArgumentException("Answers must be integers from 1 to 5.");
            }
        }
        return req.getAnswers();
    }

    private QuizDto toDto(QuizResponse r) {
        var dto = new QuizDto();
        dto.setAnswers(new ArrayList<>(r.getAnswers()));
        dto.setTotalScore(r.getAnswers().stream().mapToInt(Integer::intValue).sum());
        return dto;
    }

    private QuizDto toDto(RoomFinderQuiz q) {
        var dto = new QuizDto();
        dto.setAnswers(List.of(
                q.getQ1(), q.getQ2(), q.getQ3(), q.getQ4(), q.getQ5(),
                q.getQ6(), q.getQ7(), q.getQ8(), q.getQ9(), q.getQ10()
        ));
        dto.setTotalScore(q.getTotalScore());
        return dto;
    }

    private void setQ1toQ10(RoomFinderQuiz q, List<Integer> a) {
        q.setQ1(a.get(0));  q.setQ2(a.get(1));  q.setQ3(a.get(2));  q.setQ4(a.get(3));  q.setQ5(a.get(4));
        q.setQ6(a.get(5));  q.setQ7(a.get(6));  q.setQ8(a.get(7));  q.setQ9(a.get(8));  q.setQ10(a.get(9));
    }
}
