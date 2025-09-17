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

/**
 * MatchService with 10-question hybrid scoring.
 * Highest weights go to Budget (Q8) and Nationality (Q9).
 */
@Service
@RequiredArgsConstructor
public class MatchService {

    private final RoomFinderRepository roomFinderRepo;
    private final QuizResponseRepository quizRepo;
    private final MatchRequestRepository reqRepo;
    private final MessageService messageService;

    // ---- Quiz indices (align with your current Quiz/QuizEdit screens) ----
    // 0: Bedtime
    // 1: Cleanliness
    // 2: Noise sensitivity
    // 3: Guests frequency
    // 4: Pets tolerance
    // 5: Smoking tolerance
    // 6: Communication comfort
    // 7: Schedule alignment importance
    // 8: Budget (THB bracket)
    // 9: Nationality / region
    private static final int Q_BEDTIME = 0;
    private static final int Q_CLEAN   = 1;
    private static final int Q_NOISE   = 2;
    private static final int Q_GUESTS  = 3;
    private static final int Q_PETS    = 4;
    private static final int Q_SMOKE   = 5;
    private static final int Q_COMM    = 6;
    private static final int Q_ALIGN   = 7;
    private static final int Q_BUDGET  = 8;
    private static final int Q_NATION  = 9;

    // ---- Weights (budget & nationality highest) ----
    private static final double W_BUDGET = 4.0;
    private static final double W_NATION = 3.5;

    private static final double W_SMOKE  = 3.0;
    private static final double W_CLEAN  = 2.5;
    private static final double W_COMM   = 2.0;

    private static final double W_NOISE  = 1.5;
    private static final double W_SLEEP  = 1.5;
    private static final double W_ALIGN  = 1.2;
    private static final double W_PETS   = 1.0;
    private static final double W_GUEST  = 0.8;

    @Transactional(readOnly = true)
    public List<MatchDto> findMatchesFor(String email) {
        RoomFinder me = roomFinderRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));

        QuizResponse myQuiz = quizRepo.findByRoomFinderEmail(email).orElse(null);
        if (myQuiz == null || myQuiz.getAnswers() == null || myQuiz.getAnswers().isEmpty()) {
            return List.of();
        }
        List<Integer> myAnswers = myQuiz.getAnswers();

        List<MatchDto> out = new ArrayList<>();
        for (QuizResponse other : quizRepo.findAll()) {
            if (other == null || other.getRoomFinder() == null) continue;
            RoomFinder u = other.getRoomFinder();
            if (Objects.equals(u.getId(), me.getId())) continue; // skip self
            if (other.getAnswers() == null || other.getAnswers().isEmpty()) continue;

            int score = hybridScore10(myAnswers, other.getAnswers());

            MatchDto m = new MatchDto();
            m.setUserId(u.getId());
            m.setName(u.getName());
            m.setAge(u.getAge());
            m.setGender(u.getGender());
            m.setLocation(u.getLocation());
            m.setCompatibility(score);

            // resolve relation (request either direction)
            reqRepo.findByRequester_IdAndTarget_Id(me.getId(), u.getId())
                    .or(() -> reqRepo.findByRequester_IdAndTarget_Id(u.getId(), me.getId()))
                    .ifPresent(req -> {
                        m.setRequestId(req.getId());
                        boolean iAmRequester = req.getRequester() != null
                                && Objects.equals(req.getRequester().getId(), me.getId());
                        switch (req.getStatus()) {
                            case PENDING -> m.setRelationStatus(iAmRequester ? "PENDING_SENT" : "PENDING_RECEIVED");
                            case ACCEPTED -> {
                                m.setRelationStatus("ACCEPTED");
                                Long threadId = messageService.findExistingThreadId(me.getId(), u.getId());
                                // setThreadId only if your DTO has it
                                try {
                                    m.setThreadId(threadId);
                                } catch (NoSuchMethodError | Exception ignored) {
                                    // DTO may not have threadId; ignore if absent
                                }
                            }
                            case DECLINED -> m.setRelationStatus("DECLINED");
                        }
                    });

            if (m.getRelationStatus() == null) m.setRelationStatus("NONE");
            out.add(m);
        }

        out.sort(Comparator.comparingInt(MatchDto::getCompatibility).reversed());
        return out;
    }

    @Transactional
    public Long sendRequest(String email, Long targetId) {
        RoomFinder me = roomFinderRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        RoomFinder target = roomFinderRepo.findById(targetId).orElseThrow();

        if (reqRepo.existsByRequester_IdAndTarget_Id(me.getId(), targetId)
                || reqRepo.existsByRequester_IdAndTarget_Id(targetId, me.getId())) {
            throw new IllegalStateException("Request already exists.");
        }

        MatchRequest r = new MatchRequest();
        r.setRequester(me);
        r.setTarget(target);
        r.setStatus(MatchRequest.Status.PENDING);
        return reqRepo.save(r).getId();
    }

    @Transactional
    public void accept(String email, Long requestId) {
        MatchRequest req = reqRepo.findWithUsersById(requestId).orElseThrow();
        if (req.getTarget() == null || !email.equals(req.getTarget().getEmail())) {
            throw new SecurityException("Not allowed");
        }
        req.setStatus(MatchRequest.Status.ACCEPTED);
        reqRepo.save(req);

        // ensure chat thread exists
        messageService.ensureThread(req.getRequester().getId(), req.getTarget().getId());
    }

    @Transactional
    public void decline(String email, Long requestId) {
        MatchRequest req = reqRepo.findWithUsersById(requestId).orElseThrow();
        if (req.getTarget() == null || !email.equals(req.getTarget().getEmail())) {
            throw new SecurityException("Not allowed");
        }
        req.setStatus(MatchRequest.Status.DECLINED);
        reqRepo.save(req);
    }

    /* ----------------- Scoring helpers ----------------- */

    private int hybridScore10(List<Integer> a, List<Integer> b) {
        List<Integer> A = padTo10(a);
        List<Integer> B = padTo10(b);

        // Deal breakers
        if (isDealBreaker(A, B)) return 0;

        double total = 0.0;
        double wsum  = 0.0;

        total += factor(A, B, Q_BUDGET)  * W_BUDGET;  wsum += W_BUDGET;   // highest
        total += factor(A, B, Q_NATION)  * W_NATION;  wsum += W_NATION;   // highest

        total += factor(A, B, Q_SMOKE)   * W_SMOKE;   wsum += W_SMOKE;
        total += factor(A, B, Q_CLEAN)   * W_CLEAN;   wsum += W_CLEAN;
        total += factor(A, B, Q_COMM)    * W_COMM;    wsum += W_COMM;

        total += asymNoise(A, B)         * W_NOISE;   wsum += W_NOISE;
        total += factor(A, B, Q_BEDTIME) * W_SLEEP;   wsum += W_SLEEP;
        total += factor(A, B, Q_ALIGN)   * W_ALIGN;   wsum += W_ALIGN;
        total += factor(A, B, Q_PETS)    * W_PETS;    wsum += W_PETS;
        total += asymGuests(A, B)        * W_GUEST;   wsum += W_GUEST;

        int score = (int) Math.round((total / Math.max(1.0, wsum)) * 100.0);
        if (score < 0) score = 0;
        if (score > 100) score = 100;
        return score;
    }

    private boolean isDealBreaker(List<Integer> a, List<Integer> b) {
        // Strict non-smoker vs tolerant smoker (e.g., 5 vs <=2)
        if ((val(a, Q_SMOKE) >= 5 && val(b, Q_SMOKE) <= 2) ||
                (val(b, Q_SMOKE) >= 5 && val(a, Q_SMOKE) <= 2)) {
            return true;
        }
        // Cleanliness extreme diff at a pole
        if (Math.abs(val(a, Q_CLEAN) - val(b, Q_CLEAN)) >= 3 &&
                (val(a, Q_CLEAN) == 5 || val(b, Q_CLEAN) == 5)) {
            return true;
        }
        // Bedtime extreme mismatch (e.g., 5 vs 1)
        if (Math.abs(val(a, Q_BEDTIME) - val(b, Q_BEDTIME)) == 4) {
            return true;
        }
        return false;
    }

    /** Symmetric factor: returns 1.0 (perfect) down to 0.1 (very poor). */
    private double factor(List<Integer> a, List<Integer> b, int idx) {
        int diff = Math.abs(val(a, idx) - val(b, idx));
        if (diff == 0) return 1.00;
        if (diff == 1) return 0.85;
        if (diff == 2) return 0.60;
        if (diff == 3) return 0.30;
        return 0.10;
    }

    /** Asymmetric noise tolerance: accommodates quiet preference. */
    private double asymNoise(List<Integer> a, List<Integer> b) {
        int x = val(a, Q_NOISE), y = val(b, Q_NOISE);
        if (Math.abs(x - y) <= 1) return 1.0;
        if ((x <= 2 && y >= 4) || (y <= 2 && x >= 4)) return 0.7;
        return 0.4;
    }

    /** Asymmetric guests frequency: large mismatch penalized more. */
    private double asymGuests(List<Integer> a, List<Integer> b) {
        int x = val(a, Q_GUESTS), y = val(b, Q_GUESTS);
        int diff = Math.abs(x - y);
        if (diff <= 1) return 1.0;
        if (diff == 2) return 0.7;
        if ((x >= 4 && y <= 2) || (y >= 4 && x <= 2)) return 0.3;
        return 0.5;
    }

    private List<Integer> padTo10(List<Integer> src) {
        List<Integer> out = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            Integer v = (i < src.size()) ? src.get(i) : null;
            out.add(v == null ? 3 : v); // neutral default
        }
        return out;
    }

    private int val(List<Integer> list, int idx) {
        if (idx < 0 || idx >= list.size()) return 3;
        Integer v = list.get(idx);
        return (v == null ? 3 : v);
    }
}
