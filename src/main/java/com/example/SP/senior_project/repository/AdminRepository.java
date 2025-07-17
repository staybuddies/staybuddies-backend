package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    List<Admin> findByNameContainingIgnoreCase(String keyword);

    Optional<Admin> findByEmail(String email);
}