package com.econference.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.email")
@Getter
@Setter
public class EmailConfig {

    private String from;
    private int verificationExpiryHours = 24;
    private int resetTokenExpiryHours = 1;
    private String baseUrl = "http://localhost:3000";
}
