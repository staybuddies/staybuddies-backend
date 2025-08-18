package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    // list (non-paged) search
    List<Admin> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);

    // paged search
    Page<Admin> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);

    Optional<Admin> findByEmail(String email);

    // uniqueness check when editing (exclude current id)
    boolean existsByEmailAndIdNot(String email, Long id);
}
