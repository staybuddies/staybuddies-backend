package com.example.SP.senior_project.model;

import com.example.SP.senior_project.model.base.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "admins")
public class Admin extends AbstractAuditableEntity {

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    private String phone;
}
