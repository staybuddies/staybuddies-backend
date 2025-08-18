package com.example.SP.senior_project.service;

import com.example.SP.senior_project.dto.admin.AdminRequest;
import com.example.SP.senior_project.model.Admin;
import com.example.SP.senior_project.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    /* ---------- LIST (non-paged: used by old screens) ---------- */
    public List<Admin> getAll(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            return adminRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(kw, kw);
        }
        return adminRepository.findAll();
    }

    /* ---------- LIST (paged: used by new controller) ---------- */
    public Page<Admin> getAll(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            return adminRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(kw, kw, pageable);
        }
        return adminRepository.findAll(pageable);
    }

    /* ---------- READ ---------- */
    public Admin getById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    /* ---------- CREATE/UPDATE from DTO (simple version) ---------- */
    public void save(AdminRequest dto) {
        Admin admin;
        boolean isEdit = dto.getId() != null;

        if (isEdit) {
            admin = getById(dto.getId());

            // email uniqueness (exclude self)
            if (dto.getEmail() != null &&
                    adminRepository.existsByEmailAndIdNot(dto.getEmail(), dto.getId())) {
                throw new RuntimeException("Email already exists!");
            }

        } else {
            // creating
            admin = new Admin();

            // email uniqueness
            if (dto.getEmail() != null && adminRepository.findByEmail(dto.getEmail()).isPresent()) {
                throw new RuntimeException("Email already exists!");
            }

            // set password on create (controller already enforces non-blank)
            if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                admin.setPassword(passwordEncoder.encode(dto.getPassword()));
            }
        }

        // common fields
        admin.setName(dto.getName());
        admin.setEmail(dto.getEmail());
        admin.setPhone(dto.getPhone());

        // optional password update (not required during edit; you’re using current-password confirm in controller)
        if (!isEdit && dto.getPassword() != null && !dto.getPassword().isBlank()) {
            // already handled above for create
        } else if (isEdit && dto.getPassword() != null && !dto.getPassword().isBlank()) {
            // only encode if caller wants to actually change password (you can keep this disabled if confirmation flow differs)
            admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        adminRepository.save(admin);
    }

    /* ---------- DELETE ---------- */
    public void delete(Long id) {
        adminRepository.deleteById(id);
    }

    /* ---------- Register (explicit create path) ---------- */
    public void registerNewAdmin(Admin admin) {
        if (adminRepository.findByEmail(admin.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists!");
        }
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        adminRepository.save(admin);
    }
}
