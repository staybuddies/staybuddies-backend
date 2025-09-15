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
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class QuizInsightsService {

    private final RoomFinderRepository userRepo;
    private final QuizResponseRepository quizRepo;

    // ----- Quiz question indexes (0-based; answers are 1..5) -----
    private static final int CLEANLINESS   = 0; // 0..4 (low..high) -> answers 1..5 in DB
    private static final int NOISE_TOL     = 1; // 0..4 (needs quiet..ok with noise)
    private static final int SLEEP_TIME    = 2; // 0..4 (early..late)
    private static final int STUDY_STYLE   = 3; // 0..4 (quiet/fixed..flexible)
    private static final int GUEST_FREQ    = 4; // 0..4 (rare..frequent)
    private static final int SMOKING_OK    = 5; // 0..4 (0=no smokers, 4=ok)
    private static final int COMMUNICATION = 6; // 0..4 (poor..excellent)
    private static final int PRIVACY       = 7; // 0..4 (low..high)

    // NEW: make Q9 + Q10 special & heavier
    private static final int PRICE_RANGE   = 8; // 1..5 → budget bracket (THB)
    private static final int NATIONALITY   = 9; // 1..5 → nationality/region group

    // ----- Weights (price & nationality are strongest) -----
    private static final double PRICE_WEIGHT        = 4.0; // strongest
    private static final double NATIONALITY_WEIGHT  = 3.5; // second-strongest
    private static final double SMOKING_WEIGHT      = 3.0;
    private static final double CLEANLINESS_WEIGHT  = 2.5;
    private static final double COMMUNICATION_WEIGHT= 2.0;
    private static final double SLEEP_TIME_WEIGHT   = 1.5;
    private static final double NOISE_TOL_WEIGHT    = 1.5;
    private static final double PRIVACY_WEIGHT      = 1.2;
    private static final double STUDY_STYLE_WEIGHT  = 1.0;
    private static final double GUEST_FREQ_WEIGHT   = 0.8;

    // ----- Deal breaker thresholds (keep your originals; price/nationality weighted only) -----
    private static final int SMOKING_DEAL_BREAKER_THRESHOLD = 3;
    private static final int CLEANLINESS_DEAL_BREAKER_THRESHOLD = 3;
    private static final int COMMUNICATION_DEAL_BREAKER_THRESHOLD = 2;

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

        // Compatibility Analysis using hybrid algorithm
        dto.setProfileTags(buildProfileTags(a));
        Stats stats = computeHybridCompatibilityStats(me.getId(), a);
        dto.setAverageCompatibility(stats.avg());
        dto.setBestCompatibility(stats.best());
        dto.setHighCount(stats.high());
        dto.setTotalCompared(stats.total());

        return dto;
    }

    /* ---------- Derivations from quiz answers ---------- */

    private List<String> buildIdealTraits(List<Integer> a) {
        var out = new ArrayList<String>();
        if (val(a, CLEANLINESS) >= 3) out.add("Keeps a tidy, organized space");
        if (val(a, NOISE_TOL) <= 1) out.add("Respects quiet hours");
        if (val(a, COMMUNICATION) >= 3) out.add("Communicates issues openly");
        if (val(a, PRIVACY) >= 3) out.add("Respects personal space");
        if (val(a, SMOKING_OK) == 0) out.add("Non-smoker lifestyle");
        if (val(a, GUEST_FREQ) <= 1) out.add("Maintains a peaceful environment");

        // New, soft descriptions (just for UX)
        out.add("Budget bracket: " + describePrice(val(a, PRICE_RANGE)));
        out.add("Cultural group: " + describeNationality(val(a, NATIONALITY)));
        return out;
    }

    private List<String> buildPreferred(List<Integer> a) {
        var out = new ArrayList<String>();
        if (val(a, CLEANLINESS) >= 3) out.add("Clean and organized");
        if (val(a, NOISE_TOL) <= 1) out.add("Prefers a quiet environment");
        if (val(a, STUDY_STYLE) <= 1) out.add("Similar focused study habits");
        if (val(a, COMMUNICATION) >= 3) out.add("Good communication skills");
        if (val(a, SLEEP_TIME) <= 1) out.add("Early riser compatibility");
        if (val(a, SLEEP_TIME) >= 3) out.add("Night owl compatibility");

        // Extra hints
        out.add("Budget compatibility emphasized");
        out.add("Nationality/region compatibility emphasized");
        return out;
    }

    private List<String> buildDealBreakers(List<Integer> a) {
        var out = new ArrayList<String>();
        if (val(a, SMOKING_OK) == 0) out.add("Smokers");
        if (val(a, CLEANLINESS) >= 4 && val(a, COMMUNICATION) >= 3)
            out.add("Messy and uncommunicative roommates");
        if (val(a, GUEST_FREQ) == 0) out.add("Frequent loud parties");
        if (val(a, PRIVACY) >= 3) out.add("Disrespect for personal space");
        if (val(a, COMMUNICATION) <= 1) out.add("Poor communication");
        if (val(a, NOISE_TOL) == 0) out.add("Consistently noisy behavior");

        // NOTE: price & nationality are NOT hard deal-breakers by default.
        // If you want to make them hard blockers, uncomment in hasDealBreakers(...)
        return out;
    }

    private List<String> buildProfileTags(List<Integer> a) {
        var tags = new ArrayList<String>();

        // Sleep schedule
        if (val(a, SLEEP_TIME) <= 1) tags.add("Early bird");
        else if (val(a, SLEEP_TIME) >= 3) tags.add("Night owl");
        else tags.add("Neutral schedule");

        // Cleanliness level
        if (val(a, CLEANLINESS) >= 4) tags.add("Extremely tidy");
        else if (val(a, CLEANLINESS) >= 3) tags.add("Very tidy");
        else if (val(a, CLEANLINESS) >= 2) tags.add("Moderately tidy");
        else tags.add("Relaxed about tidiness");

        // Noise tolerance
        if (val(a, NOISE_TOL) <= 1) tags.add("Needs quiet");
        else if (val(a, NOISE_TOL) >= 3) tags.add("Very noise tolerant");
        else tags.add("Moderate noise tolerance");

        // Social activity
        if (val(a, GUEST_FREQ) >= 3) tags.add("Very social");
        else if (val(a, GUEST_FREQ) >= 2) tags.add("Moderately social");
        else tags.add("Low guest frequency");

        // Communication style
        if (val(a, COMMUNICATION) >= 3) tags.add("Great communicator");
        else if (val(a, COMMUNICATION) >= 2) tags.add("Good communicator");
        else tags.add("Reserved communicator");

        // New tags
        tags.add("Budget: " + describePrice(val(a, PRICE_RANGE)));
        tags.add("Region: " + describeNationality(val(a, NATIONALITY)));

        return tags;
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
            // Skip self
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

    /** Hybrid compatibility algorithm combining deal-breaker filtering with weighted scoring */
    private int hybridCompatibilityScore(List<Integer> a, List<Integer> b) {
        if (hasDealBreakers(a, b)) {
            return 0;
        }
        return calculateWeightedCompatibility(a, b);
    }

    /** Check for critical incompatibilities that should eliminate a match */
    private boolean hasDealBreakers(List<Integer> a, List<Integer> b) {
        // Smoking incompatibility
        if (val(a, SMOKING_OK) == 0 && val(b, SMOKING_OK) >= SMOKING_DEAL_BREAKER_THRESHOLD) return true;
        if (val(b, SMOKING_OK) == 0 && val(a, SMOKING_OK) >= SMOKING_DEAL_BREAKER_THRESHOLD) return true;

        // Extreme cleanliness mismatch
        int cleanDiff = Math.abs(val(a, CLEANLINESS) - val(b, CLEANLINESS));
        if (cleanDiff >= CLEANLINESS_DEAL_BREAKER_THRESHOLD &&
                (val(a, CLEANLINESS) == 4 || val(b, CLEANLINESS) == 4)) return true;

        // Communication breakdown potential
        if (val(a, COMMUNICATION) <= 1 && val(b, COMMUNICATION) <= 1) return true;

        // Extreme sleep schedule mismatch
        int sleepDiff = Math.abs(val(a, SLEEP_TIME) - val(b, SLEEP_TIME));
        if (sleepDiff == 4) return true;

        // OPTIONAL: Uncomment to make price a hard blocker if very far apart (>=3 brackets)
        // if (Math.abs(val(a, PRICE_RANGE) - val(b, PRICE_RANGE)) >= 3) return true;

        // OPTIONAL: Uncomment to make nationality a hard blocker if different
        // if (val(a, NATIONALITY) != 0 && val(b, NATIONALITY) != 0 && val(a, NATIONALITY) != val(b, NATIONALITY)) return true;

        return false;
    }

    /** Weighted compatibility (price & nationality dominate) */
    private int calculateWeightedCompatibility(List<Integer> a, List<Integer> b) {
        double totalScore = 0;
        double totalWeight = 0;

        // Strongest factors first
        totalScore += calculatePriceScore(a, b, PRICE_WEIGHT);
        totalWeight += PRICE_WEIGHT;

        totalScore += calculateNationalityScore(a, b, NATIONALITY_WEIGHT);
        totalWeight += NATIONALITY_WEIGHT;

        // Critical
        totalScore += calculateFactorScore(a, b, SMOKING_OK, SMOKING_WEIGHT);
        totalWeight += SMOKING_WEIGHT;

        totalScore += calculateFactorScore(a, b, CLEANLINESS, CLEANLINESS_WEIGHT);
        totalWeight += CLEANLINESS_WEIGHT;

        totalScore += calculateFactorScore(a, b, COMMUNICATION, COMMUNICATION_WEIGHT);
        totalWeight += COMMUNICATION_WEIGHT;

        // Important
        totalScore += calculateFactorScore(a, b, SLEEP_TIME, SLEEP_TIME_WEIGHT);
        totalWeight += SLEEP_TIME_WEIGHT;

        totalScore += calculateAsymmetricNoiseScore(a, b, NOISE_TOL_WEIGHT);
        totalWeight += NOISE_TOL_WEIGHT;

        totalScore += calculateFactorScore(a, b, PRIVACY, PRIVACY_WEIGHT);
        totalWeight += PRIVACY_WEIGHT;

        // Nice-to-have
        totalScore += calculateFactorScore(a, b, STUDY_STYLE, STUDY_STYLE_WEIGHT);
        totalWeight += STUDY_STYLE_WEIGHT;

        totalScore += calculateAsymmetricGuestScore(a, b, GUEST_FREQ_WEIGHT);
        totalWeight += GUEST_FREQ_WEIGHT;

        return (int) Math.round((totalScore / totalWeight) * 100);
    }

    /** Generic factor score (ordinal 1..5; smaller difference → better) */
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

    /** Price scoring — same bracket best; farther brackets drop quickly */
    private double calculatePriceScore(List<Integer> a, List<Integer> b, double weight) {
        return calculateFactorScore(a, b, PRICE_RANGE, weight);
    }

    /** Nationality scoring — equal best; adjacent groups ok; far groups lower */
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

    /** Asymmetric noise tolerance (keep your original logic) */
    private double calculateAsymmetricNoiseScore(List<Integer> a, List<Integer> b, double weight) {
        int noiseA = val(a, NOISE_TOL);
        int noiseB = val(b, NOISE_TOL);

        if (Math.abs(noiseA - noiseB) <= 1) return weight;
        if ((noiseA <= 1 && noiseB >= 3) || (noiseB <= 1 && noiseA >= 3)) return weight * 0.7;
        return weight * 0.4;
    }

    /** Asymmetric guest frequency (keep your original logic) */
    private double calculateAsymmetricGuestScore(List<Integer> a, List<Integer> b, double weight) {
        int guestA = val(a, GUEST_FREQ);
        int guestB = val(b, GUEST_FREQ);

        int diff = Math.abs(guestA - guestB);
        if (diff <= 1) return weight;
        if (diff == 2) return weight * 0.7;
        if (diff >= 3) {
            if ((guestA >= 3 && guestB <= 1) || (guestB >= 3 && guestA <= 1)) return weight * 0.3;
            return weight * 0.5;
        }
        return weight * 0.4;
    }

    /* ---------- Enhanced matching explanations ---------- */

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

        var reasons = new ArrayList<String>();

        // Strong compatibility factors
        if (close(a, b, SLEEP_TIME, 1)) reasons.add("Similar sleep schedules");
        if (close(a, b, CLEANLINESS, 1)) reasons.add("Compatible cleanliness standards");
        if (close(a, b, COMMUNICATION, 1)) reasons.add("Similar communication styles");
        if (close(a, b, STUDY_STYLE, 1)) reasons.add("Similar study/working style");
        if (close(a, b, PRIVACY, 1)) reasons.add("Similar privacy expectations");

        // Emphasize the two new heavy factors
        if (close(a, b, PRICE_RANGE, 0)) reasons.add("Same rent budget bracket");
        else if (close(a, b, PRICE_RANGE, 1)) reasons.add("Nearby rent budget brackets");

        if (close(a, b, NATIONALITY, 0)) reasons.add("Same nationality/region group");

        // Asymmetric compatibility
        if (val(a, NOISE_TOL) <= 1 && val(b, NOISE_TOL) >= 2) {
            reasons.add("They can accommodate your need for quiet");
        }
        if (val(b, NOISE_TOL) <= 1 && val(a, NOISE_TOL) >= 2) {
            reasons.add("You can accommodate their need for quiet");
        }

        // Guest frequency complementarity
        if (Math.abs(val(a, GUEST_FREQ) - val(b, GUEST_FREQ)) >= 2) {
            if (val(a, GUEST_FREQ) >= 3 && val(b, GUEST_FREQ) <= 1) {
                reasons.add("You're social, they prefer quiet - good balance");
            } else if (val(b, GUEST_FREQ) >= 3 && val(a, GUEST_FREQ) <= 1) {
                reasons.add("They're social, you prefer quiet - good balance");
            }
        }

        // No smoking conflicts
        if (val(a, SMOKING_OK) == 0 && val(b, SMOKING_OK) == 0) {
            reasons.add("Both prefer smoke-free environment");
        }

        return reasons;
    }

    /* ---------- Utility methods ---------- */

    private boolean close(List<Integer> a, List<Integer> b, int idx, int tolerance) {
        if (idx >= a.size() || idx >= b.size()) return false;
        return Math.abs(val(a, idx) - val(b, idx)) <= tolerance;
    }

    private int val(List<Integer> a, int idx) {
        // translate 1..5 answers into 0..4 internally where you had comments,
        // but keep arithmetic on 1..5; returning 0 for missing keeps it neutral/low.
        return (idx >= 0 && idx < a.size() && a.get(idx) != null) ? a.get(idx) : 0;
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

    /* ---------- Additional utility for debugging ---------- */

    public String getCompatibilityBreakdown(List<Integer> a, List<Integer> b) {
        if (hasDealBreakers(a, b)) {
            return "DEAL BREAKERS DETECTED - Match rejected";
        }

        StringBuilder breakdown = new StringBuilder();
        breakdown.append("Compatibility Breakdown:\n");
        breakdown.append(String.format("Price: %.1f/%.1f\n",
                calculatePriceScore(a, b, PRICE_WEIGHT), PRICE_WEIGHT));
        breakdown.append(String.format("Nationality: %.1f/%.1f\n",
                calculateNationalityScore(a, b, NATIONALITY_WEIGHT), NATIONALITY_WEIGHT));
        breakdown.append(String.format("Smoking: %.1f/%.1f\n",
                calculateFactorScore(a, b, SMOKING_OK, SMOKING_WEIGHT), SMOKING_WEIGHT));
        breakdown.append(String.format("Cleanliness: %.1f/%.1f\n",
                calculateFactorScore(a, b, CLEANLINESS, CLEANLINESS_WEIGHT), CLEANLINESS_WEIGHT));
        breakdown.append(String.format("Communication: %.1f/%.1f\n",
                calculateFactorScore(a, b, COMMUNICATION, COMMUNICATION_WEIGHT), COMMUNICATION_WEIGHT));
        breakdown.append(String.format("Sleep: %.1f/%.1f\n",
                calculateFactorScore(a, b, SLEEP_TIME, SLEEP_TIME_WEIGHT), SLEEP_TIME_WEIGHT));
        breakdown.append(String.format("Noise: %.1f/%.1f\n",
                calculateAsymmetricNoiseScore(a, b, NOISE_TOL_WEIGHT), NOISE_TOL_WEIGHT));
        return breakdown.toString();
    }
}
