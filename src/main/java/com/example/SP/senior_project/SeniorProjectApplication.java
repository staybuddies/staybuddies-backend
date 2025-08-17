package com.example.SP.senior_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class SeniorProjectApplication {

    public static void main(String[] args) {

        SpringApplication.run(SeniorProjectApplication.class, args);
    }
}
