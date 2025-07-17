package com.example.SP.senior_project.service;

import com.example.SP.senior_project.dto.admin.AdminRequest;
import com.example.SP.senior_project.model.Admin;
import com.example.SP.senior_project.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    @Autowired
    private final AdminRepository adminRepository;
    @Autowired
    private final PasswordEncoder passwordEncoder;

    public List<Admin> getAll(String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            return adminRepository.findByNameContainingIgnoreCase(keyword);
        } else {
            return adminRepository.findAll();
        }
    }

    public Admin getById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    public void save(AdminRequest dto) {
        Admin admin;
        if (dto.getId() != null) {
            admin = getById(dto.getId());
        } else {
            admin = new Admin();
        }
        admin.setName(dto.getName());
        admin.setEmail(dto.getEmail());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        admin.setPhone(dto.getPhone());
        adminRepository.save(admin);
    }

    public void delete(Long id) {
        adminRepository.deleteById(id);
    }

    public void registerNewAdmin(Admin admin) {
        if (adminRepository.findByEmail(admin.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists!");
        }
        // encrypt password
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));

        adminRepository.save(admin);
    }

}