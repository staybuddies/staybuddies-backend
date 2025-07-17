package com.example.SP.senior_project.controller.admin;

import com.example.SP.senior_project.model.Admin;
import com.example.SP.senior_project.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AdminService adminService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("admin", new Admin());
        return "register";
    }

    @PostMapping("/register")
    public String submitRegistrationForm(@ModelAttribute("admin") Admin admin, Model model) {
        try {
            adminService.registerNewAdmin(admin);
        } catch (RuntimeException exception) {
            model.addAttribute("registrationError", exception.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }


    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}
