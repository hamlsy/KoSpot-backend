package com.kospot.common.slack;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "slack")
public class SlackProperties {

    private boolean enabled = false;
    private Webhook webhook = new Webhook();

    @Getter
    @Setter
    public static class Webhook {
        private String errorUrl = "";
        private String registrationUrl = "";
    }
}
