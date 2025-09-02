package com.example.SP.senior_project.controller.admin;

import com.example.SP.senior_project.dto.admin.AdminUserRow;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.model.constant.FileType;
import com.example.SP.senior_project.repository.StudentIdVerificationRepository;   // <-- NEW
import com.example.SP.senior_project.service.FileService;
import com.example.SP.senior_project.service.RoomFinderAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final RoomFinderAdminService roomFinderAdminService;
    private final FileService fileService;
    private final StudentIdVerificationRepository idvRepo;        // <-- NEW

    /* -------- LIST (search + pagination + sorting) -------- */
    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       @RequestParam(value = "sort", defaultValue = "id") String sort,
                       @RequestParam(value = "dir", defaultValue = "desc") String dir,
                       Model model) {

        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(direction, sort));

        Page<RoomFinder> pageData = roomFinderAdminService.getAll(keyword, pageable);

        // Build view rows with verification info
        List<AdminUserRow> rows = pageData.getContent().stream().map(u -> {
            var r = new AdminUserRow();
            r.setId(u.getId());
            r.setEmail(u.getEmail());
            r.setName(u.getName());
            r.setJoinDate(u.getJoinDate());
            r.setActive(u.isActive());
            r.setEmailVerified(Boolean.TRUE.equals(u.isSchoolEmailVerified())); // email step

            var idv = idvRepo.findTopByUser_IdOrderByCreatedAtDesc(u.getId()).orElse(null);
            r.setIdvStatus(idv != null ? idv.getStatus().name() : null); // ID step

            return r;
        }).toList();

        model.addAttribute("pageData", pageData);
        model.addAttribute("users", rows);       // <-- pass rows instead of RoomFinder entities
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);

        return "admin/user-management";
    }

    /* -------- VIEW (unchanged) -------- */
    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model) {
        RoomFinder user = roomFinderAdminService.getById(id);
        String photoUrl;
        try {
            photoUrl = fileService.getFileName(FileType.ROOMFINDER_PROFILE, user.getId());
        } catch (Exception ex) {
            photoUrl = "/images/default-avatar.png";
        }
        model.addAttribute("user", user);
        model.addAttribute("photoUrl", photoUrl);
        return "admin/user-details";
    }

    // ... the rest of your controller stays the same ...
}
