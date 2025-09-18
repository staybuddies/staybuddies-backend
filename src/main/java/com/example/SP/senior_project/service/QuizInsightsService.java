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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class QuizInsightsService {

    private final RoomFinderRepository userRepo;
    private final QuizResponseRepository quizRepo;

    // ----- Quiz question indexes (MUST match UI order) -----
    private static final int SLEEP_TIME       = 0; // 1..5 (1=very late, 5=very early)
    private static final int CLEANLINESS      = 1; // 1..5 (messy .. very tidy)
    private static final int NOISE_QUIET_PREF = 2; // 1..5 (not sensitive .. very sensitive / needs quiet)
    private static final int GUEST_FREQ       = 3; // 1..5 (many guests .. few guests)
    private static final int PETS_OK          = 4; // 1..5 (1=no pets, 5=loves pets)
    private static final int SMOKING_TOL      = 5; // 1..5 (1=don’t care, 5=no smoking at all)
    private static final int COMMUNICATION    = 6; // 1..5 (1=avoids, 5=very open)
    private static final int SCHED_ALIGN_IMP  = 7; // 1..5 (1=doesn’t matter, 5=very important)
    private static final int PRICE_RANGE      = 8; // 1..5 budget bracket
    private static final int NATIONALITY      = 9; // 1..5 region bucket

    // ----- Weights -----
    private static final double PRICE_WEIGHT        = 4.0; // strongest
    private static final double NATIONALITY_WEIGHT  = 3.5; // second-strongest
    private static final double SMOKING_WEIGHT      = 3.0;
    private static final double CLEANLINESS_WEIGHT  = 2.5;
    private static final double COMMUNICATION_WEIGHT= 2.0;
    private static final double SLEEP_TIME_WEIGHT   = 1.5;
    private static final double NOISE_WEIGHT        = 1.5;
    private static final double GUEST_FREQ_WEIGHT   = 0.8;

    // ----- Deal-breaker heuristics -----
    private static final int CLEANLINESS_DEAL_BREAKER_DIFF = 3;

    @Transactional(readOnly = true)
    public QuizInsightsDto insightsFor(String email) {
        RoomFinder me = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        QuizResponse mine = quizRepo.findByRoomFinderEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        List<Integer> a = mine.getAnswers();
        var dto = new QuizInsightsDto();

        dto.setIdealTraits(buildIdealTraits(a));
        dto.setPreferredCharacteristics(buildPreferred(a));
        dto.setDealBreakers(buildDealBreakers(a));

        dto.setProfileTags(buildProfileTags(a));
        Stats stats = computeHybridCompatibilityStats(me.getId(), a);
        dto.setAverageCompatibility(stats.avg());
        dto.setBestCompatibility(stats.best());
        dto.setHighCount(stats.high());
        dto.setTotalCompared(stats.total());

        return dto;
    }

    /* ---------- User-facing tags derived from answers ---------- */

    private List<String> buildProfileTags(List<Integer> a) {
        var tags = new ArrayList<String>();

        // Sleep schedule (5 early, 1 late)
        if (val(a, SLEEP_TIME) >= 4) tags.add("Early bird");
        else if (val(a, SLEEP_TIME) <= 2) tags.add("Night owl");
        else tags.add("Neutral schedule");

        // Cleanliness
        int cl = val(a, CLEANLINESS);
        if (cl >= 5) tags.add("Extremely tidy");
        else if (cl == 4) tags.add("Very tidy");
        else if (cl == 3) tags.add("Moderately tidy");
        else tags.add("Relaxed about tidiness");

        // Noise (higher = needs quiet)
        int nq = val(a, NOISE_QUIET_PREF);
        if (nq >= 4) tags.add("Needs quiet");
        else if (nq <= 2) tags.add("Very noise tolerant");
        else tags.add("Moderate noise tolerance");

        // Guests (higher = fewer guests)
        int gf = val(a, GUEST_FREQ);
        if (gf <= 2) tags.add("Very social");
        else if (gf == 3) tags.add("Moderately social");
        else tags.add("Low guest frequency");

        // Pets
        int p = val(a, PETS_OK);
        if (p >= 4) tags.add("Pet friendly");
        else if (p == 3) tags.add("Neutral with pets");
        else tags.add("Prefers no pets");

        // Smoking (higher = less tolerant)
        int sm = val(a, SMOKING_TOL);
        if (sm >= 5) tags.add("Smoke-free home");
        else if (sm == 4) tags.add("Outdoor only");
        else tags.add("Some smoking tolerance");

        // Communication
        int cm = val(a, COMMUNICATION);
        if (cm >= 4) tags.add("Great communicator");
        else if (cm == 3) tags.add("Neutral communicator");
        else tags.add("Avoids confrontation");

        // Schedule alignment
        int sa = val(a, SCHED_ALIGN_IMP);
        if (sa >= 4) tags.add("Values aligned schedules");
        else if (sa <= 2) tags.add("Flexible schedules");
        else tags.add("Some schedule alignment");

        // Budget & Region (exact labels)
        tags.add("Budget: " + describePrice(val(a, PRICE_RANGE)));
        tags.add("Region: " + describeNationality(val(a, NATIONALITY)));

        return tags;
    }

    private List<String> buildIdealTraits(List<Integer> a) {
        var out = new ArrayList<String>();
        if (val(a, CLEANLINESS) >= 4) out.add("Keeps a tidy, organized space");
        if (val(a, NOISE_QUIET_PREF) >= 4) out.add("Respects quiet hours");
        if (val(a, COMMUNICATION) >= 4) out.add("Communicates issues openly");
        if (val(a, GUEST_FREQ) >= 4) out.add("Maintains a peaceful environment");
        out.add("Budget bracket: " + describePrice(val(a, PRICE_RANGE)));
        out.add("Cultural group: " + describeNationality(val(a, NATIONALITY)));
        return out;
    }

    private List<String> buildPreferred(List<Integer> a) {
        var out = new ArrayList<String>();
        if (val(a, CLEANLINESS) >= 4) out.add("Clean and organized");
        if (val(a, NOISE_QUIET_PREF) >= 4) out.add("Prefers a quiet environment");
        if (val(a, COMMUNICATION) >= 4) out.add("Good communication skills");
        if (val(a, SLEEP_TIME) >= 4) out.add("Early riser compatibility");
        if (val(a, SLEEP_TIME) <= 2) out.add("Night owl compatibility");
        out.add("Budget compatibility emphasized");
        out.add("Nationality/region compatibility emphasized");
        return out;
    }

    private List<String> buildDealBreakers(List<Integer> a) {
        var out = new ArrayList<String>();
        if (val(a, SMOKING_TOL) >= 5) out.add("No smoking at all at home");
        out.add("Extreme cleanliness mismatch");
        if (val(a, COMMUNICATION) <= 2) out.add("Poor communication");
        if (val(a, GUEST_FREQ) <= 2 && val(a, NOISE_QUIET_PREF) >= 4) {
            out.add("Frequent loud parties");
        }
        return out;
    }

    /** Used by PublicProfileService when it only has a userId. */
    @Transactional(readOnly = true)
    public List<String> tagsForUser(Long userId) {
        var qr = quizRepo.findByRoomFinder_Id(userId).orElse(null);
        if (qr == null || qr.getAnswers() == null || qr.getAnswers().isEmpty()) return List.of();
        return buildProfileTags(qr.getAnswers());
    }

    /** Also exposed so callers can compute tags from a raw answers array. */
    public List<String> tagsFromAnswers(List<Integer> answers) {
        if (answers == null || answers.isEmpty()) return List.of();
        return buildProfileTags(answers);
    }

    @Transactional(readOnly = true)
    public List<String> whyYouMatch(String myEmailOrNull, Long otherId) {
        if (myEmailOrNull == null || otherId == null) return List.of();

        var me    = quizRepo.findByRoomFinderEmail(myEmailOrNull).orElse(null);
        var other = quizRepo.findByRoomFinder_Id(otherId).orElse(null);
        if (me == null || other == null) return List.of();
        var a = me.getAnswers();
        var b = other.getAnswers();
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return List.of();

        var reasons = new ArrayList<String>();

        // Core similarities (±1 tolerance)
        if (close(a, b, SLEEP_TIME, 1))    reasons.add("Similar sleep schedules");
        if (close(a, b, CLEANLINESS, 1))   reasons.add("Compatible cleanliness standards");
        if (close(a, b, COMMUNICATION, 1)) reasons.add("Similar communication style");
        if (close(a, b, NOISE_QUIET_PREF, 1))
            reasons.add("Similar noise/quiet preference");

        // Schedule alignment importance
        if (close(a, b, SCHED_ALIGN_IMP, 1) ||
                (val(a, SCHED_ALIGN_IMP) >= 4 && val(b, SCHED_ALIGN_IMP) >= 4)) {
            reasons.add("Both value aligned schedules");
        }

        // Budget (strong factor)
        int priceDiff = Math.abs(val(a, PRICE_RANGE) - val(b, PRICE_RANGE));
        if (priceDiff == 0)      reasons.add("Same rent budget bracket");
        else if (priceDiff == 1) reasons.add("Nearby rent budget brackets");

        // Nationality / region
        if (val(a, NATIONALITY) == val(b, NATIONALITY)) {
            reasons.add("Same nationality/region group");
        }

        // Pets
        int pa = val(a, PETS_OK), pb = val(b, PETS_OK);
        if (pa >= 4 && pb >= 4)       reasons.add("Both are pet friendly");
        else if (pa <= 2 && pb <= 2)  reasons.add("Both prefer no pets");

        // Smoking (higher = less tolerant)
        if (val(a, SMOKING_TOL) >= 4 && val(b, SMOKING_TOL) >= 4) {
            reasons.add("Both prefer a smoke-free home");
        }

        // Guests
        if (close(a, b, GUEST_FREQ, 1)) {
            reasons.add("Similar hosting/guest frequency");
        } else if ((val(a, GUEST_FREQ) >= 4 && val(b, GUEST_FREQ) <= 2) ||
                (val(b, GUEST_FREQ) >= 4 && val(a, GUEST_FREQ) <= 2)) {
            reasons.add("Good balance of social and quiet time");
        }

        // Keep it succinct
        return reasons.size() > 8 ? reasons.subList(0, 8) : reasons;
    }

    /** helper: “are these two answers within N steps of each other?” */
    private boolean close(List<Integer> a, List<Integer> b, int idx, int tolerance) {
        if (a == null || b == null) return false;
        if (idx < 0 || idx >= a.size() || idx >= b.size()) return false;
        return Math.abs(val(a, idx) - val(b, idx)) <= tolerance;
    }

    /* ---------- Hybrid Compatibility Algorithm ---------- */

    private record Stats(int avg, int best, long high, long total) {}

    private Stats computeHybridCompatibilityStats(Long meUserId, List<Integer> myAnswers) {
        int best = 0;
        long total = 0;
        long high = 0;
        int sum = 0;

        for (QuizResponse q : quizRepo.findAll()) {
            if (q == null || q.getAnswers() == null || q.getAnswers().isEmpty()) continue;
            if (q.getRoomFinder() != null && Objects.equals(q.getRoomFinder().getId(), meUserId)) continue;

            int s = hybridCompatibilityScore(myAnswers, q.getAnswers());
            sum += s;
            best = Math.max(best, s);
            if (s >= 80) high++;
            total++;
        }

        int avg = total == 0 ? 0 : Math.round(sum / (float) total);
        return new Stats(avg, best, high, total);
    }

    private int hybridCompatibilityScore(List<Integer> a, List<Integer> b) {
        if (hasDealBreakers(a, b)) return 0;
        return calculateWeightedCompatibility(a, b);
    }

    private boolean hasDealBreakers(List<Integer> a, List<Integer> b) {
        int cleanDiff = Math.abs(val(a, CLEANLINESS) - val(b, CLEANLINESS));
        if (cleanDiff >= CLEANLINESS_DEAL_BREAKER_DIFF &&
                (val(a, CLEANLINESS) == 5 || val(b, CLEANLINESS) == 5)) return true;

        if (val(a, COMMUNICATION) <= 2 && val(b, COMMUNICATION) <= 2) return true;

        if (Math.abs(val(a, SLEEP_TIME) - val(b, SLEEP_TIME)) == 4) return true;

        if ((val(a, SMOKING_TOL) >= 5 && val(b, SMOKING_TOL) <= 2) ||
                (val(b, SMOKING_TOL) >= 5 && val(a, SMOKING_TOL) <= 2)) return true;

        return false;
    }

    private int calculateWeightedCompatibility(List<Integer> a, List<Integer> b) {
        double totalScore = 0;
        double totalWeight = 0;

        totalScore += calculateFactorScore(a, b, PRICE_RANGE, PRICE_WEIGHT);
        totalWeight += PRICE_WEIGHT;

        totalScore += calculateNationalityScore(a, b, NATIONALITY_WEIGHT);
        totalWeight += NATIONALITY_WEIGHT;

        totalScore += calculateFactorScore(a, b, SMOKING_TOL, SMOKING_WEIGHT);
        totalWeight += SMOKING_WEIGHT;

        totalScore += calculateFactorScore(a, b, CLEANLINESS, CLEANLINESS_WEIGHT);
        totalWeight += CLEANLINESS_WEIGHT;

        totalScore += calculateFactorScore(a, b, COMMUNICATION, COMMUNICATION_WEIGHT);
        totalWeight += COMMUNICATION_WEIGHT;

        totalScore += calculateFactorScore(a, b, SLEEP_TIME, SLEEP_TIME_WEIGHT);
        totalWeight += SLEEP_TIME_WEIGHT;

        totalScore += calculateAsymmetricNoiseScore(a, b, NOISE_WEIGHT);
        totalWeight += NOISE_WEIGHT;

        totalScore += calculateAsymmetricGuestScore(a, b, GUEST_FREQ_WEIGHT);
        totalWeight += GUEST_FREQ_WEIGHT;

        return (int) Math.round((totalScore / totalWeight) * 100);
    }

    private double calculateFactorScore(List<Integer> a, List<Integer> b, int index, double weight) {
        int valA = val(a, index);
        int valB = val(b, index);
        int diff = Math.abs(valA - valB);

        if (diff == 0) return weight;         // perfect
        if (diff == 1) return weight * 0.85;  // good
        if (diff == 2) return weight * 0.60;  // moderate
        if (diff == 3) return weight * 0.30;  // poor
        return weight * 0.10;                 // very poor
    }

    private double calculateNationalityScore(List<Integer> a, List<Integer> b, double weight) {
        int va = val(a, NATIONALITY);
        int vb = val(b, NATIONALITY);
        int diff = Math.abs(va - vb);

        if (diff == 0) return weight;
        if (diff == 1) return weight * 0.80;
        if (diff == 2) return weight * 0.55;
        if (diff == 3) return weight * 0.35;
        return weight * 0.20;
    }

    private double calculateAsymmetricNoiseScore(List<Integer> a, List<Integer> b, double weight) {
        int na = val(a, NOISE_QUIET_PREF);
        int nb = val(b, NOISE_QUIET_PREF);
        int diff = Math.abs(na - nb);

        if (diff <= 1) return weight;
        if ((na >= 4 && nb <= 2) || (nb >= 4 && na <= 2)) return weight * 0.4;
        return weight * 0.7;
    }

    private double calculateAsymmetricGuestScore(List<Integer> a, List<Integer> b, double weight) {
        int ga = val(a, GUEST_FREQ);
        int gb = val(b, GUEST_FREQ);
        int diff = Math.abs(ga - gb);

        if (diff <= 1) return weight;
        if (diff == 2) return weight * 0.7;
        if ((ga <= 2 && gb >= 4) || (gb <= 2 && ga >= 4)) return weight * 0.3;
        return weight * 0.5;
    }

    /* ---------- Utility ---------- */

    private boolean isZeroBased(List<Integer> a) {
        if (a == null || a.isEmpty()) return false;
        Integer max = a.stream().filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(0);
        boolean has5 = a.stream().anyMatch(v -> v != null && v == 5);
        return !has5 && max != null && max <= 4; // 0..4 with no 5 present
    }

    private int val(List<Integer> a, int idx) {
        if (a == null || idx < 0 || idx >= a.size() || a.get(idx) == null) return 3; // neutral default
        int v = a.get(idx);
        if (isZeroBased(a)) v = v + 1; // only shift if the WHOLE list is 0..4
        if (v < 1) v = 1;
        if (v > 5) v = 5;
        return v;
    }

    private String describePrice(int v) {
        return switch (v) {
            case 1 -> "≤ 4,000 THB";
            case 2 -> "4,001–6,000 THB";
            case 3 -> "6,001–8,000 THB";
            case 4 -> "8,001–10,000 THB";
            case 5 -> "> 10,000 THB";
            default -> "unspecified";
        };
    }

    private String describeNationality(int v) {
        return switch (v) {
            case 1 -> "Thai";
            case 2 -> "ASEAN (non-Thai)";
            case 3 -> "East/South Asia";
            case 4 -> "Europe/Americas/Oceania";
            case 5 -> "Other / Prefer not to say";
            default -> "unspecified";
        };
    }

    public String getCompatibilityBreakdown(List<Integer> a, List<Integer> b) {
        if (hasDealBreakers(a, b)) return "DEAL BREAKERS DETECTED - Match rejected";

        StringBuilder breakdown = new StringBuilder();
        breakdown.append("Compatibility Breakdown:\n");
        breakdown.append(String.format("Price: %.1f/%.1f\n",
                calculateFactorScore(a, b, PRICE_RANGE, PRICE_WEIGHT), PRICE_WEIGHT));
        breakdown.append(String.format("Nationality: %.1f/%.1f\n",
                calculateNationalityScore(a, b, NATIONALITY_WEIGHT), NATIONALITY_WEIGHT));
        breakdown.append(String.format("Smoking: %.1f/%.1f\n",
                calculateFactorScore(a, b, SMOKING_TOL, SMOKING_WEIGHT), SMOKING_WEIGHT));
        breakdown.append(String.format("Cleanliness: %.1f/%.1f\n",
                calculateFactorScore(a, b, CLEANLINESS, CLEANLINESS_WEIGHT), CLEANLINESS_WEIGHT));
        breakdown.append(String.format("Communication: %.1f/%.1f\n",
                calculateFactorScore(a, b, COMMUNICATION, COMMUNICATION_WEIGHT), COMMUNICATION_WEIGHT));
        breakdown.append(String.format("Sleep: %.1f/%.1f\n",
                calculateFactorScore(a, b, SLEEP_TIME, SLEEP_TIME_WEIGHT), SLEEP_TIME_WEIGHT));
        breakdown.append(String.format("Noise: %.1f/%.1f\n",
                calculateAsymmetricNoiseScore(a, b, NOISE_WEIGHT), NOISE_WEIGHT));
        breakdown.append(String.format("Guests: %.1f/%.1f\n",
                calculateAsymmetricGuestScore(a, b, GUEST_FREQ_WEIGHT), GUEST_FREQ_WEIGHT));
        return breakdown.toString();
    }
}
