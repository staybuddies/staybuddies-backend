package com.example.SP.senior_project.service;

import com.example.SP.senior_project.dto.match.MatchDto;
import com.example.SP.senior_project.model.MatchRequest;
import com.example.SP.senior_project.model.QuizResponse;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.MatchRequestRepository;
import com.example.SP.senior_project.repository.QuizResponseRepository;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final RoomFinderRepository roomFinderRepo;
    private final QuizResponseRepository quizRepo;
    private final MatchRequestRepository reqRepo;
    private final MessageService messageService; // new

    @Transactional(readOnly = true)
    public List<MatchDto> findMatchesFor(String email) {
        RoomFinder me = roomFinderRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));

        QuizResponse myQuiz = quizRepo.findByRoomFinderEmail(email).orElse(null);
        if (myQuiz == null) return List.of();

        return quizRepo.findAll().stream()
                .filter(q -> !q.getRoomFinder().getId().equals(me.getId()))
                .map(other -> toMatchDto(me, myQuiz, other))
                .sorted(Comparator.comparing(MatchDto::getCompatibility).reversed())
                .toList();
    }

    private MatchDto toMatchDto(RoomFinder me, QuizResponse mine, QuizResponse other) {
        var u = other.getRoomFinder();
        var m = new MatchDto();
        m.setUserId(u.getId());
        m.setName(u.getName());
        m.setAge(u.getAge());
        m.setGender(u.getGender());
        m.setLocation(u.getLocation());
        m.setCompatibility(score(mine.getAnswers(), other.getAnswers()));

        reqRepo.findByRequester_IdAndTarget_Id(me.getId(), u.getId())
                .or(() -> reqRepo.findByRequester_IdAndTarget_Id(u.getId(), me.getId()))
                .ifPresent(req -> {
                    m.setRequestId(req.getId());
                    boolean iAmRequester = req.getRequester().getId().equals(me.getId());
                    switch (req.getStatus()) {
                        case PENDING -> m.setRelationStatus(iAmRequester ? "PENDING_SENT" : "PENDING_RECEIVED");
                        case ACCEPTED -> {
                            m.setRelationStatus("ACCEPTED");
                            // expose thread id if it exists
                            Long threadId = messageService.findExistingThreadId(me.getId(), u.getId());
                            m.setThreadId(threadId);
                        }
                        case DECLINED -> m.setRelationStatus("DECLINED");
                    }
                });

        if (m.getRelationStatus() == null) m.setRelationStatus("NONE");
        return m;
    }

    private int score(List<Integer> a, List<Integer> b) {
        double diff = 0;
        for (int i = 0; i < a.size(); i++) diff += Math.abs(a.get(i) - b.get(i));
        return (int) Math.round(100 - (diff / a.size() / 4.0) * 100.0);
    }

    @Transactional
    public Long sendRequest(String email, Long targetId) {
        RoomFinder me = roomFinderRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        RoomFinder target = roomFinderRepo.findById(targetId).orElseThrow();

        if (reqRepo.existsByRequester_IdAndTarget_Id(me.getId(), targetId)
                || reqRepo.existsByRequester_IdAndTarget_Id(targetId, me.getId()))
            throw new IllegalStateException("Request already exists.");

        var r = new MatchRequest();
        r.setRequester(me);
        r.setTarget(target);
        r.setStatus(MatchRequest.Status.PENDING);
        return reqRepo.save(r).getId();
    }

    @Transactional
    public void accept(String email, Long requestId) {
        var req = reqRepo.findWithUsersById(requestId).orElseThrow();
        if (!req.getTarget().getEmail().equals(email)) throw new SecurityException("Not allowed");
        req.setStatus(MatchRequest.Status.ACCEPTED);
        reqRepo.save(req);

        // ensure a chat thread exists for these two users
        messageService.ensureThread(req.getRequester().getId(), req.getTarget().getId());
        // optional: create a notification for requester here
    }

    @Transactional
    public void decline(String email, Long requestId) {
        var req = reqRepo.findWithUsersById(requestId).orElseThrow();
        if (!req.getTarget().getEmail().equals(email)) throw new SecurityException("Not allowed");
        req.setStatus(MatchRequest.Status.DECLINED);
        reqRepo.save(req);
    }
}

