package com.example.SP.senior_project.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPagesController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalAdmins", 0);
        model.addAttribute("newReports", 0);
        model.addAttribute("activeConvos", 0);
        return "admin/dashboard";
    }

    @GetMapping("/reports")
    public String reports() {
        return "admin/reports";
    }

    @GetMapping("/messages")
    public String messages() {
        return "admin/messages";
    }

    @GetMapping("/analytics")
    public String analytics() {
        return "admin/analytics";
    }

    @GetMapping("/settings")
    public String settings() {
        return "admin/settings";
    }
}
