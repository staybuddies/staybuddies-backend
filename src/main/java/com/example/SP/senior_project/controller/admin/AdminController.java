package com.example.SP.senior_project.controller.admin;

import com.example.SP.senior_project.dto.admin.AdminRequest;
import com.example.SP.senior_project.model.Admin;
import com.example.SP.senior_project.model.constant.FileType;
import com.example.SP.senior_project.repository.AdminRepository;
import com.example.SP.senior_project.service.AdminService;
import com.example.SP.senior_project.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admins")
@RequiredArgsConstructor
public class AdminController {

    @Autowired
    private final AdminService adminService;
    @Autowired
    private final AdminRepository adminRepository;
    @Autowired
    private final FileService fileService;
    @Autowired
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        model.addAttribute("admins", adminService.getAll(keyword));
        model.addAttribute("keyword", keyword);
        return "admin/admin-management";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model) {
        Admin admin = adminService.getById(id);
        model.addAttribute("admin", adminService.getById(id));
        String photoUrl;
        try {
            photoUrl = fileService.getFileName(FileType.ADMIN_PROFILE, admin.getId());
        } catch (Exception ex) {
            photoUrl = "/images/default-avatar.png";
        }

        model.addAttribute("admin", admin);
        model.addAttribute("photoUrl", photoUrl);

        return "admin/admin-details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("adminRequest", new AdminRequest());
        return "admin/admin-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Admin admin = adminService.getById(id);
        AdminRequest dto = new AdminRequest();
        dto.setId(admin.getId());
        dto.setName(admin.getName());
        dto.setEmail(admin.getEmail());
        dto.setPhone(admin.getPhone());
        String photoUrl = null;
        try {
            photoUrl = fileService.getFileName(FileType.ADMIN_PROFILE, admin.getId());
        } catch (Exception ex) {
            photoUrl = "/images/default-avatar.png";
        }
        model.addAttribute("adminRequest", dto);
        model.addAttribute("photoUrl", photoUrl);
        model.addAttribute("id", admin.getId());
        return "admin/admin-form";
    }

    //    @PostMapping("/save")
//    public String save(@ModelAttribute AdminRequest adminRequest) {
//
//        Admin admin;
//
//        if (adminRequest.getId() != null) {
//            // existing admin
//            admin = adminRepository.findById(adminRequest.getId())
//                    .orElseThrow(() -> new RuntimeException("Admin not found"));
//        } else {
//            admin = new Admin();
//        }
//        admin.setName(adminRequest.getName());
//        admin.setEmail(adminRequest.getEmail());
//        admin.setPhone(adminRequest.getPhone());
//        if (adminRequest.getPassword() != null && !adminRequest.getPassword().isBlank()) {
//            String hashed = passwordEncoder.encode(adminRequest.getPassword());
//            admin.setPassword(hashed);
//        }
//        admin = adminRepository.save(admin);
//
//        // 2. Handle file upload if file exists
//        if (adminRequest.getFile() != null && !adminRequest.getFile().isEmpty()) {
//            fileService.handleFileUpload(
//                    adminRequest.getFile(),
//                    FileType.ADMIN_PROFILE, // your enum value
//                    admin.getId(),
//                    "s3" // or "local" depending on your app config
//            );
//        }
//        return "redirect:/admins";
//    }
    @PostMapping("/save")
    public String save(@ModelAttribute AdminRequest adminRequest, Model model) {
        Admin admin;

        if (adminRequest.getId() != null) {
            // existing admin
            admin = adminRepository.findById(adminRequest.getId())
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            // ✅ Validate password
            if (!passwordEncoder.matches(adminRequest.getPassword(), admin.getPassword())) {
                model.addAttribute("adminRequest", adminRequest);
                model.addAttribute("id", admin.getId());
                model.addAttribute("photoUrl", fileService.getFileName(FileType.ADMIN_PROFILE, admin.getId()));
                model.addAttribute("error", "Incorrect password. Please enter your current password to confirm.");
                return "admin/admin-form";
            }

        } else {
            // creating new admin → must set new password
            if (adminRequest.getPassword() == null || adminRequest.getPassword().isBlank()) {
                model.addAttribute("adminRequest", adminRequest);
                model.addAttribute("error", "Password is required when creating a new admin.");
                return "admin/admin-form";
            }

            admin = new Admin();
            admin.setPassword(passwordEncoder.encode(adminRequest.getPassword()));
        }

        // Common: update info
        admin.setName(adminRequest.getName());
        admin.setEmail(adminRequest.getEmail());
        admin.setPhone(adminRequest.getPhone());

        admin = adminRepository.save(admin);

        // Optional photo upload
        if (adminRequest.getFile() != null && !adminRequest.getFile().isEmpty()) {
            fileService.handleFileUpload(adminRequest.getFile(), FileType.ADMIN_PROFILE, admin.getId(), "s3");
        }

        return "redirect:/admins";
    }


    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        adminService.delete(id);
        return "redirect:/admins";
    }
}