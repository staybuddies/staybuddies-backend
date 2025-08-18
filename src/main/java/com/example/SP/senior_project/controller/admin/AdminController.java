package com.example.SP.senior_project.controller.admin;

import com.example.SP.senior_project.dto.admin.AdminRequest;
import com.example.SP.senior_project.model.Admin;
import com.example.SP.senior_project.model.constant.FileType;
import com.example.SP.senior_project.repository.AdminRepository;
import com.example.SP.senior_project.service.AdminService;
import com.example.SP.senior_project.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    /* -------------------- LIST (with pagination & sorting) -------------------- */
    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       @RequestParam(value = "sort", defaultValue = "id") String sort,
                       @RequestParam(value = "dir", defaultValue = "desc") String dir,
                       Model model) {

        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(direction, sort));

        Page<Admin> pageData = adminService.getAll(keyword, pageable);

        model.addAttribute("pageData", pageData); // for pagination UI
        model.addAttribute("admins", pageData.getContent()); // for existing table
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "admin/admin-management";
    }

    /* -------------------- VIEW -------------------- */
    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model) {
        Admin admin = adminService.getById(id);

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

    /* -------------------- CREATE FORM -------------------- */
    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("adminRequest")) {
            model.addAttribute("adminRequest", new AdminRequest());
        }
        return "admin/admin-form";
    }

    /* -------------------- EDIT FORM -------------------- */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Admin admin = adminService.getById(id);

        if (!model.containsAttribute("adminRequest")) {
            AdminRequest dto = new AdminRequest();
            dto.setId(admin.getId());
            dto.setName(admin.getName());
            dto.setEmail(admin.getEmail());
            dto.setPhone(admin.getPhone());
            model.addAttribute("adminRequest", dto);
        }

        String photoUrl;
        try {
            photoUrl = fileService.getFileName(FileType.ADMIN_PROFILE, admin.getId());
        } catch (Exception ex) {
            photoUrl = "/images/default-avatar.png";
        }

        model.addAttribute("photoUrl", photoUrl);
        model.addAttribute("id", admin.getId());
        return "admin/admin-form";
    }

    /* -------------------- SAVE (create & update) -------------------- */
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("adminRequest") AdminRequest adminRequest,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes ra) {

        // If creating a new admin, password is required (also enforced by @Valid)
        boolean isEdit = adminRequest.getId() != null;

        Admin admin;
        if (isEdit) {
            admin = adminRepository.findById(adminRequest.getId())
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            // For edits, require current password as confirmation (matches existing)
            if (adminRequest.getPassword() == null || adminRequest.getPassword().isBlank()
                    || !passwordEncoder.matches(adminRequest.getPassword(), admin.getPassword())) {

                binding.rejectValue("password", "invalid.confirm",
                        "Incorrect current password. Please enter your current password to confirm.");
            }
        } else {
            admin = new Admin();
            // For create, set encoded password
            if (adminRequest.getPassword() != null && !adminRequest.getPassword().isBlank()) {
                admin.setPassword(passwordEncoder.encode(adminRequest.getPassword()));
            }
        }

        // Bean validation errors?
        if (binding.hasErrors()) {
            // Re-attach photo preview if present (for edit form)
            if (isEdit) {
                try {
                    model.addAttribute("photoUrl", fileService.getFileName(FileType.ADMIN_PROFILE, admin.getId()));
                } catch (Exception ex) {
                    model.addAttribute("photoUrl", "/images/default-avatar.png");
                }
                model.addAttribute("id", admin.getId());
            }
            return "admin/admin-form";
        }

        // Common updates
        admin.setName(adminRequest.getName());
        admin.setEmail(adminRequest.getEmail());
        admin.setPhone(adminRequest.getPhone());

        admin = adminRepository.save(admin);

        // Optional photo upload
        if (adminRequest.getFile() != null && !adminRequest.getFile().isEmpty()) {
            fileService.handleFileUpload(adminRequest.getFile(), FileType.ADMIN_PROFILE, admin.getId(), "s3");
        }

        ra.addFlashAttribute("success",
                isEdit ? "Admin updated successfully." : "Admin created successfully.");
        return "redirect:/admins";
    }

    /* -------------------- DELETE -------------------- */
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        adminService.delete(id);
        ra.addFlashAttribute("success", "Admin deleted.");
        return "redirect:/admins";
    }

    /* -------------------- Graceful errors -> list page -------------------- */
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntime(RuntimeException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:/admins";
    }


}
