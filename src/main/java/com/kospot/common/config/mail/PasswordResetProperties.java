package com.kospot.common.config.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {
    private String baseUrl;
    private String fromEmail;
    private long tokenTtlMinutes;
    private int rateLimitMax;
    private long rateLimitTtlHours;
}
