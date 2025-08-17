package com.example.SP.senior_project.service;

import com.example.SP.senior_project.dto.quiz.QuizInsightsDto;
import com.example.SP.senior_project.model.QuizResponse;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.QuizResponseRepository;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizInsightsService {

    private final RoomFinderRepository userRepo;
    private final QuizResponseRepository quizRepo;

    // ----- Adjust these indexes to your actual quiz -----
    private static final int CLEANLINESS = 0; // 0..4 (low..high)
    private static final int NOISE_TOL = 1; // 0..4 (needs quiet..ok with noise)
    private static final int SLEEP_TIME = 2; // 0..4 (early..late)
    private static final int STUDY_STYLE = 3; // 0..4 (quiet/fixed..flexible)
    private static final int GUEST_FREQ = 4; // 0..4 (rare..frequent)
    private static final int SMOKING_OK = 5; // 0..4 (0=no smokers, 4=ok)
    private static final int COMMUNICATION = 6; // 0..4 (poor..excellent)
    private static final int PRIVACY = 7; // 0..4 (low..high)
    // ----------------------------------------------------

    @Transactional(readOnly = true)
    public QuizInsightsDto insightsFor(String email) {
        RoomFinder me = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        QuizResponse mine = quizRepo.findByRoomFinderEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        List<Integer> a = mine.getAnswers();
        var dto = new QuizInsightsDto();

        // Roommate Preferences
        dto.setIdealTraits(buildIdealTraits(a));
        dto.setPreferredCharacteristics(buildPreferred(a));
        dto.setDealBreakers(buildDealBreakers(a));

        // Compatibility Analysis
        dto.setProfileTags(buildProfileTags(a));
        var stats = computeCompatibilityStats(a);
        dto.setAverageCompatibility(stats.avg);
        dto.setBestCompatibility(stats.best);
        dto.setHighCount(stats.high);
        dto.setTotalCompared(stats.total);

        return dto;
    }

    /* ---------- Derivations from quiz answers ---------- */

    private List<String> buildIdealTraits(List<Integer> a) {
        var out = new ArrayList<String>();
        if (val(a, CLEANLINESS) >= 3) out.add("Keeps a tidy, organized space");
        if (val(a, NOISE_TOL) <= 1) out.add("Respects quiet hours");
        if (val(a, COMMUNICATION) >= 3) out.add("Communicates issues openly");
        if (val(a, PRIVACY) >= 3) out.add("Respects personal space");
        return out;
    }

    private List<String> buildPreferred(List<Integer> a) {
        var out = new ArrayList<String>();
        if (val(a, CLEANLINESS) >= 3) out.add("Clean and organized");
        if (val(a, NOISE_TOL) <= 1) out.add("Prefers a quiet environment");
        if (val(a, STUDY_STYLE) <= 1) out.add("Similar focused study habits");
        if (val(a, COMMUNICATION) >= 3) out.add("Good communication");
        return out;
    }

    private List<String> buildDealBreakers(List<Integer> a) {
        var out = new ArrayList<String>();
        if (val(a, SMOKING_OK) == 0) out.add("Smokers");
        if (val(a, GUEST_FREQ) >= 3) out.add("Frequent loud parties");
        if (val(a, PRIVACY) >= 3) out.add("Disrespect for personal space");
        if (val(a, COMMUNICATION) <= 1) out.add("Poor communication");
        return out;
    }

    private List<String> buildProfileTags(List<Integer> a) {
        var tags = new ArrayList<String>();
        // Sleep
        if (val(a, SLEEP_TIME) <= 1) tags.add("Early bird");
        else if (val(a, SLEEP_TIME) >= 3) tags.add("Night owl");
        else tags.add("Neutral schedule");
        // Cleanliness
        tags.add(val(a, CLEANLINESS) >= 3 ? "Very tidy" : "More relaxed about tidiness");
        // Noise
        tags.add(val(a, NOISE_TOL) <= 1 ? "Needs quiet" : "Okay with some noise");
        // Social/guests
        tags.add(val(a, GUEST_FREQ) >= 3 ? "Very social" : "Low guest frequency");
        return tags;
    }

    /* ---------- Compatibility vs everyone else ---------- */

    private record Stats(int avg, int best, long high, long total) {
    }

    private Stats computeCompatibilityStats(List<Integer> me) {
        List<QuizResponse> all = quizRepo.findAll();
        int best = 0;
        long count = 0;
        long high = 0;
        int sum = 0;

        for (QuizResponse q : all) {
            if (q.getAnswers() == null || q.getAnswers().isEmpty()) continue;
            // skip self (no email on QuizResponse? compare lists length & identity)
            if (q.getId().equals(q.getId())) { /* noop; leave as is */ }

            if (q.getId() == null) continue;
            if (q.getRoomFinder() != null && q.getRoomFinder().getEmail() != null
                    && q.getRoomFinder().getEmail().equalsIgnoreCase(meEmailSafe(me))) {
                continue; // skip self if resolvable
            }

            int score = score(me, q.getAnswers());
            sum += score;
            best = Math.max(best, score);
            if (score >= 80) high++;
            count++;
        }

        int avg = count == 0 ? 0 : Math.round(sum / (float) count);
        return new Stats(avg, best, high, count);
    }

    // If you want a true self skip, pass email down; this adapter keeps compile simple
    private String meEmailSafe(List<Integer> me) {
        return "";
    }

    @Transactional(readOnly = true)
    public List<String> tagsForUser(Long userId) {
        var qr = quizRepo.findByRoomFinder_Id(userId).orElse(null);
        if (qr == null || qr.getAnswers() == null) return List.of();
        return buildProfileTags(qr.getAnswers());
    }

    @Transactional(readOnly = true)
    public List<String> whyYouMatch(String myEmail, Long otherUserId) {
        var me = quizRepo.findByRoomFinderEmail(myEmail).orElse(null);
        var other = quizRepo.findByRoomFinder_Id(otherUserId).orElse(null);
        if (me == null || other == null) return List.of();

        var a = me.getAnswers();
        var b = other.getAnswers();

        // simple, readable explanations
        var out = new ArrayList<String>();
        if (close(a, b, SLEEP_TIME, 1)) out.add("Similar sleep schedules");
        if (close(a, b, CLEANLINESS, 1)) out.add("Compatible cleanliness standards");
        if (close(a, b, NOISE_TOL, 1)) out.add("Both prefer quiet environments");
        if (close(a, b, STUDY_STYLE, 1)) out.add("Similar study/working style");
        if (close(a, b, PRIVACY, 1)) out.add("Similar privacy expectations");
        if (far(a, b, GUEST_FREQ, 2)) out.add("Different guest frequency");
        return out;
    }

    private boolean close(List<Integer> a, List<Integer> b, int idx, int tol) {
        if (idx >= a.size() || idx >= b.size()) return false;
        return Math.abs(val(a, idx) - val(b, idx)) <= tol;
    }
    private boolean far(List<Integer> a, List<Integer> b, int idx, int tol) {
        if (idx >= a.size() || idx >= b.size()) return false;
        return Math.abs(val(a, idx) - val(b, idx)) >= tol;
    }



    private int score(List<Integer> a, List<Integer> b) {
        int n = Math.min(a.size(), b.size());
        if (n == 0) return 0;
        double diff = 0;
        for (int i = 0; i < n; i++) diff += Math.abs(a.get(i) - b.get(i));
        return (int) Math.round(100 - (diff / n / 4.0) * 100.0);
    }

    private int val(List<Integer> a, int idx) {
        return (idx >= 0 && idx < a.size() && a.get(idx) != null) ? a.get(idx) : 0;
    }
}
