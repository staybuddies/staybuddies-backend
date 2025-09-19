package com.example.SP.senior_project.service;

import com.example.SP.senior_project.model.MatchRequest;
import com.example.SP.senior_project.repository.MatchRequestRepository;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final RoomFinderRepository users;
    private final MatchRequestRepository requests;

    @PersistenceContext
    private EntityManager entityManager;

    /* ---------- DTOs ---------- */
    public record Kpis(
            long totalUsers, long activeUsers, long matchesMade, long newUsersToday,
            String totalUsersDelta, String activeUsersNote, String matchRateNote, String newUsersDelta
    ) {}

    public record Growth(List<String> labels, List<Integer> values) {}

    /* ---------- KPIs ---------- */
    @Transactional(readOnly = true)
    public Kpis getKpis() {
        long totalUsers  = users.count();
        long activeUsers = safe(() -> users.countByActiveTrue(), 0L);

        // Unique ACCEPTED pairs (A,B) == (B,A)
        List<Object[]> edges = entityManager.createQuery("""
            select mr.requester.id, mr.target.id
            from MatchRequest mr
            where mr.status = :st
        """, Object[].class)
                .setParameter("st", MatchRequest.Status.ACCEPTED)
                .getResultList();

        Set<String> uniquePairs = new HashSet<>();
        Set<Long> matchedUsers  = new HashSet<>();
        for (Object[] row : edges) {
            long a = (Long) row[0], b = (Long) row[1];
            long lo = Math.min(a, b), hi = Math.max(a, b);
            uniquePairs.add(lo + "-" + hi);
            matchedUsers.add(lo); matchedUsers.add(hi);
        }
        long matchesMade  = uniquePairs.size();
        long matchedCount = matchedUsers.size();

        // New users today
        long newUsersToday = countUsersOn(LocalDate.now());

        // Notes
        String activeNote = totalUsers == 0 ? "0% of total users"
                : Math.round(activeUsers * 100.0 / totalUsers) + "% of total users";
        String matchRate  = totalUsers == 0 ? "0% match rate"
                : Math.round(matchedCount * 100.0 / totalUsers) + "% match rate";

        // Optional deltas (fill however you want)
        String totalDelta = "+12.5% from last month";
        String newDelta   = "+0% from yesterday";

        return new Kpis(totalUsers, activeUsers, matchesMade, newUsersToday,
                totalDelta, activeNote, matchRate, newDelta);
    }

    /* ---------- Growth series for chart ---------- */
    @Transactional(readOnly = true)
    public Growth getGrowth(String range) {
        LocalDate end = LocalDate.now();
        LocalDate start = switch (range == null ? "WEEK" : range.toUpperCase()) {
            case "MONTH"   -> end.minusDays(29);
            case "QUARTER" -> end.minusDays(89);
            case "YEAR"    -> end.minusDays(364);
            default        -> end.minusDays(6); // WEEK
        };

        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            labels.add(d.toString());
            values.add((int) countUsersOn(d));
        }
        return new Growth(labels, values);
    }

    /* ---------- Helpers ---------- */

    // Works whether your join date is LocalDate or LocalDateTime
    private long countUsersOn(LocalDate day) {
        try {
            // If you have: long countByJoinDate(LocalDate date)
            return users.countByJoinDate(day);
        } catch (Exception ignored) {
            // Fallback if joinDate is LocalDateTime
            LocalDateTime from = day.atStartOfDay();
            LocalDateTime to   = from.plusDays(1);
            return users.countByJoinDateBetween(from, to);
        }
    }

    private static <T> T safe(SupplierWithEx<T> f, T fallback) {
        try { return f.get(); } catch (Exception ignored) { return fallback; }
    }
    @FunctionalInterface private interface SupplierWithEx<T> { T get() throws Exception; }
}
