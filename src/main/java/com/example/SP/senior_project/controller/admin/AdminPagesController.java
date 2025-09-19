package com.example.SP.senior_project.controller.admin;

import com.example.SP.senior_project.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPagesController {

    private final AdminDashboardService dashboard;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "range", defaultValue = "WEEK") String range, Model model) {
        // KPIs
        var m = dashboard.getKpis();
        model.addAttribute("totalUsers",     m.totalUsers());
        model.addAttribute("activeUsers",    m.activeUsers());
        model.addAttribute("matchesMade",    m.matchesMade());
        model.addAttribute("newUsersToday",  m.newUsersToday());

        model.addAttribute("totalUsersDelta", m.totalUsersDelta()); // e.g. "+12.5% from last month"
        model.addAttribute("activeUsersNote", m.activeUsersNote()); // e.g. "70% of total users"
        model.addAttribute("matchRateNote",   m.matchRateNote());   // e.g. "35% match rate"
        model.addAttribute("newUsersDelta",   m.newUsersDelta());   // e.g. "+8% from yesterday"

        // Growth series for chart
        var g = dashboard.getGrowth(range);
        model.addAttribute("growthLabels", g.labels());
        model.addAttribute("growthValues", g.values());

        // Keep the selected range so <option th:selected> works
        model.addAttribute("range", range);

        return "admin/dashboard";
    }
}
