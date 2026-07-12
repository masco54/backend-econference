package com.econference.event.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.event-request")
@Getter
@Setter
public class EventRequestConfig {

    private String trackingCodePrefix = "SOL";
    private int activationExpiryHours = 168;
    private String activationPath = "/organizer-activation";
    private boolean notificationsEnabled = true;
    private String baseUrl = "http://localhost:4200";
}