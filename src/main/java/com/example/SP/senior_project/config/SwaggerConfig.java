package com.example.SP.senior_project.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stay-Buddies Backend API")
                        .version("1.0")
                        .description("API Documents for the Project")
                        .contact(new Contact().name("Zwe Nyan Win").email("zwenaynwin001@gmail.com")));
    }
}
